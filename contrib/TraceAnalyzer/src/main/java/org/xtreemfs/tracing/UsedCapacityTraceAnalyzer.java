/*
 * Copyright (c) 2008-2014 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.tracing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            LongWritable offset = new LongWritable();
            offset.set(getOffset(value.toString()));
            Text file = new Text();
            file.set(getFileId(value.toString()));
            context.write(file, offset);
        }
    }

    private class UsedCapacityTraceAnalyzerReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
        private Map<String, Long> maxOffsets;

        @Override
        protected void setup(Context context) {
            this.maxOffsets = new HashMap<String, Long>();
        }

        @Override
        protected void cleanup(Context context)  throws IOException, InterruptedException {
            long sum = 0;

            for(String key: this.maxOffsets.keySet()) {
                sum += this.maxOffsets.get(key);
            }

            context.write(new Text("Used capacity"), new LongWritable(sum));
        }

        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long maxOffset = 0;
            for(LongWritable value: values) {
                maxOffset = Math.max(value.get(), maxOffset);
            }
            if(this.maxOffsets.containsKey(key.toString())) {
                this.maxOffsets.put(key.toString(), Math.max(maxOffset, this.maxOffsets.get(key.toString())));
            } else {
                this.maxOffsets.put(key.toString(), maxOffset);
            }
        }
    }

    private class UsedCapacityTraceAnalyzerCombiner extends Reducer<Text, LongWritable, Text, LongWritable> {
        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
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
        job.setCombinerClass(UsedCapacityTraceAnalyzerCombiner.class);

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        // One reduce task to get single result
        job.setNumReduceTasks(1);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        job.waitForCompletion(true);
    }
}
