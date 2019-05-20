import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.chain.ChainMapper;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UnitMultiplication {

    public static class TransitionMapper extends Mapper<Object, Text, Text, Text> {

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            //input : fromPage\t toPage1,toPage2,toPage3
            //target: build transition matrix unit -> fromPage\t toPage=probability

            String[] ind = value.toString().trim().split("\t");
            if(ind[1].trim().equals("") || (ind.length == 1)) {
                return;
            } else {
                String from = ind[0];
                String[] to = ind[1].split(",");
                for (String t: to) {
                    context.write(new Text(from),new Text(t + "=" + (double)1/to.length));
                }
            }

        }
    }

    public static class PRMapper extends Mapper<Object, Text, Text, Text> {

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            //input : Page\t PageRank
            //target: write to reducer
            String[] val = value.toString().trim().split("\t");
            context.write(new Text(val[0]),new Text(val[1]));
        }
    }

    public static class MultiplicationReducer extends Reducer<Text, Text, Text, Text> {


        @Override
        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {

            //input key = fromPage value=<toPage=probability,..., pageRank>
            //target: get the unit multiplication
                    double pageRank = 0;
                    List<String> lPages = new ArrayList<String> ();
                    
                    for (Text val: values) {
                        String tmp = val.toString();
                        if (tmp.contains("=")) {
                            lPages.add(val.toString());
                        } else {
                            lPages.add(val.toString());
                            pageRank = Double.parseDouble(val.toString());
                        }
                    }
                     for (String cell: lPages) {
                        double relationValue = Double.parseDouble(cell.split("=")[1]);
                        String outputKey = cell.split("=")[0];
                        String outputValue = String.valueOf(relationValue*pageRank);
                        context.write(new Text(outputKey),new Text(outputValue));
                     }

        }
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf);
        job.setJarByClass(UnitMultiplication.class);

        ChainMapper.addMapper(job,TransitionMapper.class,Object.class,Text.class,Text.class,Text.class,conf);
        ChainMapper.addMapper(job,PRMapper.class,Object.class,Text.class,Text.class,Text.class,conf);

        job.setReducerClass(MultiplicationReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, TransitionMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, PRMapper.class);

        FileOutputFormat.setOutputPath(job, new Path(args[2]));
        job.waitForCompletion(true);
    }

}
