package com.my.project;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.junit.Test;

/**
 * MapReduce程序示例
 * @author yang
 *
 */
public class TestMapReduce {

	/**
	 * 在本地执行MapReduce程序（JobId带有local字样），数据和计算结果均存放在本地
	 * @throws Exception
	 */
	@Test
	public void testMapReduceLocal() throws Exception {

		Configuration conf = new Configuration();

		Job job = Job.getInstance(conf);
		job.setMapperClass(WordMapper.class);
		job.setReducerClass(WordReducer.class);

		//设定Mapper的输出key-value类型
		//若Mapper与Job的输出key-value类型相同，则可省略此步
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);

		//设定Job的输出key-value类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		FileInputFormat.setInputPaths(job, "BUILD.txt");
		FileOutputFormat.setOutputPath(job, new Path("target/output/"));

		job.waitForCompletion(true);
	}

	/**
	 * 在本地执行MapReduce程序（JobId仍带有local字样），从HDFS上获取数据，计算结果存放到HDFS上
	 * 注意：如果输出目录已存在则会执行失败
	 * @throws Exception
	 */
	@Test
	public void testMapReduceOnHdfs() throws Exception {

		Configuration conf = new Configuration();

		Job job = Job.getInstance(conf);
		job.setMapperClass(WordMapper.class);
		job.setReducerClass(WordReducer.class);

		//设定Mapper的输出key-value类型
		//若Mapper与Job的输出key-value类型相同，则可省略此步
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);

		//设定Job的输出key-value类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		FileInputFormat.setInputPaths(job, "hdfs://192.168.56.10:9000/input/");
		FileOutputFormat.setOutputPath(job, new Path("hdfs://192.168.56.10:9000/output1/"));

		job.waitForCompletion(true);
	}

	/**
	 * 在Yarn上执行MapReduce程序（JobId不带有local字样），从HDFS上获取数据，计算结果存放到HDFS上
	 * <pre>
	 * 1.需要提前将MapReduce程序打成jar包：mvn clean package -Dmaven.test.skip=true
	 * 2.然后指定mapreduce.job.jar参数
	 * 3.这个jar包会被上传到ResourceManager
	 * 4.在任务执行时再由ResourceManager分发到NodeManager上
	 * 注意：如果输出目录已存在则会执行失败
	 * </pre>
	 * @throws Exception
	 */
	@Test
	public void testMapReduceOnYarn() throws Exception {

		Configuration conf = new Configuration();
		/*
		conf.set("fs.defaultFS", "hdfs://192.168.56.10:9000/");
		conf.set("mapreduce.job.jar", "target/hello-hadoop-0.0.1.jar");
		conf.set("mapreduce.framework.name", "yarn");
		conf.set("yarn.resourcemanager.hostname", "192.168.56.10");
		conf.set("mapreduce.app-submission.cross-platform", "true");
		*/
		//从core-site.xml、hdfs-site.xml、mapred-site.xml、yarn-site.xml读取以上配置

		Job job = Job.getInstance(conf);
		job.setMapperClass(WordMapper.class);
		job.setReducerClass(WordReducer.class);

		//设定Mapper的输出key-value类型
		//若Mapper与Job的输出key-value类型相同，则可省略此步
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(IntWritable.class);

		//设定Job的输出key-value类型
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(LongWritable.class);

		FileInputFormat.setInputPaths(job, "/input/");
		FileOutputFormat.setOutputPath(job, new Path("/output2/"));

		job.waitForCompletion(true);
	}

}
