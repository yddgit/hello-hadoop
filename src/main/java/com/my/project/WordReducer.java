package com.my.project;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Reducer
 * @author yang
 *
 */
public class WordReducer extends Reducer<Text, IntWritable, Text, LongWritable> {

	@Override
	protected void reduce(Text key, Iterable<IntWritable> values,
			Reducer<Text, IntWritable, Text, LongWritable>.Context context) throws IOException, InterruptedException {
		long count = 0;
		for(IntWritable value : values) {
			count += value.get();
		}

		context.write(key, new LongWritable(count));
	}

}
