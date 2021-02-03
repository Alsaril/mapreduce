package ru.mail;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.json.JSONObject;

import java.io.IOException;

public class WordCount extends Configured implements Tool {
    private static class WordCountMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] splits = line.split("\\W");
            for (String word : splits) {
                if (word.length() > 0) {
                    context.write(new Text(word), new LongWritable(1));
                } else {
                    context.getCounter("mapper", "empty_words").increment(1);
                }
            }
        }
    }

    private static class WordCountReducer extends Reducer<Text, LongWritable, Text, NullWritable> {
        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long count = 0;
            for (LongWritable c : values) {
                count += c.get();
            }

            JSONObject obj = new JSONObject();
            obj.put(key.toString(), count);

            context.write(new Text(obj.toString()), NullWritable.get());
        }
    }

    public static void main(String[] args) throws Exception {
        ToolRunner.run(new WordCount(), args);
    }

    @Override
    public int run(String[] args) throws Exception {
        Job job = Job.getInstance(getConf());
        job.setJarByClass(WordCount.class);

        job.setInputFormatClass(TextInputFormat.class);
        TextInputFormat.addInputPath(job, new Path("/data/big.txt"));

        job.setMapperClass(WordCountMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);

        job.setReducerClass(WordCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        job.setOutputFormatClass(TextOutputFormat.class);
        TextOutputFormat.setOutputPath(job, new Path("/user/igor.yakovlev/output"));

        if (!job.waitForCompletion(true)) {
            throw new IllegalStateException("Job failed!");
        }

        return 0;
    }
}
