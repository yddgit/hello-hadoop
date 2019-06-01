package com.my.project;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FSEditLog原理理解：双缓冲、分段锁
 */
public class FSEditLog {

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		System.out.println("start: " + start);
		FSEditLog fsEditLog = new FSEditLog();
		int i = 0;
		while (i++ < 100) {
			new Thread(() -> {
				int j = 0;
				while(j++ < 100) {
					fsEditLog.writeLog(Thread.currentThread().getName() + ":" + j);
				}
			}, "T" + i).start();
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("use time: " + (System.currentTimeMillis() - start))));
	}

	/** 事务ID */
	public long taxid = 0;
	/** 双缓冲 */
	public DoubleBuffer doubleBuffer = new DoubleBuffer();
	/** 当前是否有线程在刷写磁盘 */
	public boolean isRunning = false;
	/** 当前批次最大的事务ID */
	public long maxTaxid = 0;
	/** 当前线程的事务ID */
	public ThreadLocal<Long> threadLocalTaxid = new ThreadLocal<Long>();
	/** 是否有线程在等待刷写磁盘 */
	public boolean isWait = false;

	/** For Test：同步磁盘的次数 */
	public AtomicLong flushCount = new AtomicLong(0);
	/** For Test: 同步磁盘的数据记录数 */
	public AtomicLong recordCount = new AtomicLong(0);
	/** For Test: 批次内记录数 */
	public AtomicLong batchCount = new AtomicLong(0);
	/** For Test: 新批次记录数 */
	public AtomicLong newBatchCount = new AtomicLong(0);

	/**
	 * 分段加锁
	 */
	public void writeLog(String log) {
		synchronized (this) {
			// 高并发场景下瞬间有大量的请求进到这段代码块，很快将数据写入内存
			taxid++; // 获取事务ID
			EditLog editLog=new EditLog(taxid, log); // 创建数据对象
			doubleBuffer.writeLog(editLog);	//写内存
			threadLocalTaxid.set(taxid); // 设置当前线程的事务ID
		} // 写内存结束，释放锁，【避免】磁盘IO耗时过长导致大量请求等待锁无法得到处理
		flushLog(); // 写磁盘：某一个线程首先执行到这段代码时，当前接收请求数据的buffer里可能已经写了很多数据了
	}

	/**
	 * 将数据同步到磁盘
	 */
	private void flushLog() {
		synchronized (this) {
			// 当前有线程正在同步数据到磁盘
			if (isRunning) {

				long localTaxid = threadLocalTaxid.get();

				// 如果当前线程事务ID小于当前批次最大的事务ID
				// 则说明已经【有线程在同步当前批次的数据】，直接返回
				if (localTaxid <= maxTaxid) {
					batchCount.incrementAndGet();
					return;
				}

				// 如果是新批次的数据，而且已经有线程在等待刷写磁盘，直接返回
				// 因为新批次数据的其他线程进入时都会判断如果已经有线程在等待时会直接返回
				if (isWait) {
					newBatchCount.incrementAndGet();
					return; // 凡是在这里返回的数据都会和等待同步数据线程的数据划分为同一批次
				}

				// 如果当前线程事务ID大于当前批次最大的事务ID，则说明这是一个【新批次】
				// 而且没有线程在等待刷写【新批次】的数据，则说明当前线程是【新批次】数据中第一个走到该代码段的线程
				// 又因为现在【已经有】线程在同步数据到磁盘了（即上个批次还没同步完成），所以开始【等待】
				isWait = true;
				while (isRunning) { //当前数据同步未结束（即上个批次还没同步完成之前），一直循环等待
					try {
						this.wait(1000); // 等待的时候释放锁，让线程其他线程仍能正常处理数据
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// 一旦线程被唤醒或wait()方法超时，发现当前数据同步已结束，则停止等待
				isWait = false;

			}
			// 交换内存，新批次数据交换到同步数据的buffer
			doubleBuffer.exchange();
			// 开始同步新批次数据
			isRunning = true;
			if (doubleBuffer.syncBuffer.size() != 0) {
				maxTaxid = doubleBuffer.getMaxTaxid(); // 设置当前批次最大的事务ID
			}

		} //释放锁，开始同步数据到磁盘

		doubleBuffer.flush(); // 同步磁盘过程缓慢，不加锁，不影响其他请求的处理

		synchronized (this) {
			// 当前线程结束同步
			isRunning=false;
			System.out.println(flushCount.get() + "\t"
				+ "flushed\t" + recordCount.get()
				+ "\ttaxid\t" + taxid
				+ "\tmaxTaxid\t" + maxTaxid
				+ "\tbatchCount\t" + batchCount.get()
				+ "\tnewBatchCount\t" + newBatchCount.get()
				+ "\ttime\t" + System.currentTimeMillis());
			// 唤醒等待同步的线程开始新批次的同步
			this.notifyAll();
		}

	}

	/**
	 * 两块内存buffer，一块用于接收请求数据，一块用于同步磁盘
	 * 当接收请求数据的buffer写满时，则将其交换到同步磁盘的buffer，接收请求的buffer继续接收请求不受影响
	 * 其他线程将同步磁盘的buffer中的数据刷写到磁盘
	 */
	public class DoubleBuffer {
		/** 当前用来写日志的内存消息队列 */
		public LinkedList<EditLog> currentBuffer = new LinkedList<EditLog>();
		/** 用于将内存里面的数据同步到磁盘的队列 */
		public LinkedList<EditLog> syncBuffer = new LinkedList<EditLog>();

		/**
		 * 写日志（写内存）
		 * @param editLog
		 */
		public void writeLog(EditLog editLog) {
			currentBuffer.add(editLog);
		}

		/**
		 * 写数据到磁盘（写磁盘，毫秒级）
		 */
		public void flush() {
			// 模拟写磁盘20ms-100ms写完
			try {
				int interval = new Random().nextInt(100);
				interval = interval < 20 ? 20 : interval;
				TimeUnit.MICROSECONDS.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (EditLog editLog : syncBuffer) {
				editLog.toString();
				// System.out.println(editLog);
			}
			// 写磁盘完成
			flushCount.incrementAndGet();
			recordCount.addAndGet(syncBuffer.size());
			syncBuffer.clear();
		}

		/**
		 * 交换内存
		 */
		public void exchange() {
			LinkedList<EditLog> tmp = currentBuffer;
			currentBuffer = syncBuffer;
			syncBuffer = tmp;
		}

		/**
		 * 获取当前要同步到磁盘的数据中最大的事务ID
		 */
		public long getMaxTaxid() {
			return syncBuffer.getLast().taxid;
		}
	}	

	/**
	 * 一条数据
	 */
	public class EditLog {
		public long taxid;
		public String log;

		public EditLog(long taxid, String log) {
			this.taxid = taxid;
			this.log = log;
		}

		@Override
		public String toString() {
			return "EditLog [taxid=" + taxid + ", log=" + log + "]";
		}
	}

}
