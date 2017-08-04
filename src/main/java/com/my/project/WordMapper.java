package com.my.project;

import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Mapper
 * @author yang
 *
 */
public class WordMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

	@Override
	protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, IntWritable>.Context context)
			throws IOException, InterruptedException {

		final IntWritable ONE = new IntWritable(1);

		String s = value.toString();
		String[] words = s.split(" ");
		for(String word : words) {
			context.write(new Text(word), ONE);
		}
	}

}
