/*
 * Copyright (c) 2008-2014 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.tracing;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class UsedCapacityTraceAnalyzer {
    private class UsedCapacityTraceAnalyzerMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
        private long getOffset(String input) {
            //TODO: Adapt to actual trace format
            return 1;
        }

        private String getFileId(String input) {
            //TODO: Adapt to actual trace format
            return "1";
        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            LongWritable offset = new LongWritable();
            offset.set(getOffset(value.toString()));
            Text file = new Text();
            file.set(getFileId(value.toString()));
            context.write(file, offset);
        }
    }

    private class UsedCapacityTraceAnalyzerReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long maxOffset = 0;
            for(LongWritable value: values) {
                maxOffset = Math.max(value.get(), maxOffset);
            }
            context.write(key, new LongWritable(maxOffset));
        }
    }

    public static void main(String args[]) throws Exception {
        Configuration conf = new Configuration();

        Job job = new Job(conf, "UsedCapacityTraceAnalyzer");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        job.setMapperClass(UsedCapacityTraceAnalyzerMapper.class);
        job.setReducerClass(UsedCapacityTraceAnalyzerReducer.class);
        job.setCombinerClass(UsedCapacityTraceAnalyzerReducer.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.waitForCompletion(true);
    }
}
