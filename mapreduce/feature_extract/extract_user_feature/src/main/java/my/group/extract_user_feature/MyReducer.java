package my.group.extract_user_feature;

import com.aliyun.odps.data.Record;
import com.aliyun.odps.mapred.Reducer;

import java.awt.List;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Reducer模板。请用真实逻辑替换模板内容
 */
public class MyReducer implements Reducer {
	private Record result;

    public void setup(TaskContext context) throws IOException {
    	result = context.createOutputRecord();
    }

    public void reduce(Record key, Iterator<Record> values, TaskContext context) throws IOException {
    	long[] near_buy_time = new long[2];//用户第一次/最近一次购买任意品牌时间
    	long[] near_cart_time = new long[2];//用户第一次/最近一次加入购物车任意品牌时间
    	long[] near_contact_time = new long[2];//用户第一次/最近一次交互任意品牌时间
    	long[] buy_count = new long[5];//用户最近1/3/7/15/30天购买所有品牌的次数
    	long[] cart_count = new long[5];//用户最近1/3/7/15/30天加入购物车所有品牌的次数
    	long[] beh_count = new long[5];//用户最近1/3/7/15/30天交互所有品牌的次数
    	long[] buy_items = new long[5];//用户最近1/3/7/15/30天购买不同品牌的个数
    	long[] cart_items = new long[5];//用户最近1/3/7/15/30天加入购物车不同品牌的个数
    	long[] beh_items = new long[5];//用户最近1/3/7/15/30天交互过不同品牌的个数
    	long[] buy_days = new long[3];//用户最近7/15/30天有多少天购买任意品牌
    	long[] cart_days = new long[3];//用户最近7/15/30天有多少天加入购物车任意品牌
    	long[] beh_days = new long[3];//用户最近7/15/30天有多少天交互过任意品牌
    	double buy_count_per_item = 0.0, beh_count_per_item = 0.0;
    	double user_buy_count_ratio = 1.0, user_buy_day_ratio = 1.0, user_buy_items_ratio = 1.0;
    	double buy_frequency = 0.0f;
    	long[] buy_flag = new long[5]; 
    	near_buy_time[0] = -1L; near_buy_time[1] = 720L;
    	near_cart_time[0] = -1L; near_cart_time[1] = 720L;
    	near_contact_time[0] =  -1L; near_contact_time[1] = 720L;
    	
    	Set[] cart_items_set = new HashSet[5], buy_items_set = new HashSet[5], beh_items_set = new HashSet[5];
    	for(int i = 0; i < 5; ++i){
    		buy_items_set[i] = new HashSet();
    		cart_items_set[i] = new HashSet();
    		beh_items_set[i] = new HashSet();
    	}
    	boolean[] cart_day_flag = new boolean[30], buy_day_flag = new boolean[30], beh_day_flag = new boolean[30];
    	ArrayList buy_gap = new ArrayList();
    	
    	while(values.hasNext()){
    		Record val = values.next();
    		String item_id = val.getString("item_id");
			String date = val.getString("time");
//			String day_str = date.substring(0, 10);
			int type = Integer.parseInt(val.getString("behavior_type")) - 1;
			long t_hour = getHourSpan("2014-12-18 00", date);
			int t_day = (int)t_hour / 24;
			if(t_hour <= 0 || t_hour >= 720)
				continue;
			if(type == 2){
				if(t_hour < near_cart_time[1])
					near_cart_time[1] = t_hour;
				if(t_hour > near_cart_time[0])
					near_cart_time[0] = t_hour;
				if(t_hour < 24){
					cart_count[0] += 1;
					cart_items_set[0].add(item_id);
				}
				if(t_hour < 72){
					cart_count[1] += 1;
					cart_items_set[1].add(item_id);
				}
				if(t_hour < 168){
					cart_count[2] += 1;
					cart_items_set[2].add(item_id);
				}
				if(t_hour < 360){
					cart_count[3] += 1;
					cart_items_set[3].add(item_id);
				}
				cart_count[4] += 1;
				cart_items_set[4].add(item_id);
				cart_day_flag[t_day] = true;
				
//				buy_gap.add(t_day);
			}
			else if(type == 3){
				if(t_hour < near_buy_time[1])
					near_buy_time[1] = t_hour;
				if(t_hour > near_buy_time[0])
					near_buy_time[0] = t_hour;
				if(t_hour < 24){
					buy_count[0] += 1;
					buy_items_set[0].add(item_id);
					buy_flag[0] = 1;
				}
				if(t_hour < 72){
					buy_count[1] += 1;
					buy_items_set[1].add(item_id);
					buy_flag[1] = 1;
				}
				if(t_hour < 168){
					buy_count[2] += 1;
					buy_items_set[2].add(item_id);
					buy_flag[2] = 1;
				}
				if(t_hour < 360){
					buy_count[3] += 1;
					buy_items_set[3].add(item_id);
					buy_flag[3] = 1;
				}
				buy_count[4] += 1;
				buy_items_set[4].add(item_id);
				buy_day_flag[t_day] = true;
				
//				buy_gap.add(t_day);
				
				buy_flag[4] = 1;
			}
//			else{
				if(t_hour < near_contact_time[1])
					near_contact_time[1] = t_hour;
				if(t_hour > near_contact_time[0])
					near_contact_time[0] = t_hour;
				if(t_hour < 24){
					beh_count[0] += 1;
					beh_items_set[0].add(item_id);
				}
				if(t_hour < 72){
					beh_count[1] += 1;
					beh_items_set[1].add(item_id);
				}
				if(t_hour < 168){
					beh_count[2] += 1;
					beh_items_set[2].add(item_id);
				}
				if(t_hour < 360){
					beh_count[3] += 1;
					beh_items_set[3].add(item_id);
				}
				beh_count[4] += 1;
				beh_items_set[4].add(item_id);
				beh_day_flag[t_day] = true;
//			}
    	}
    	for(int i = 0; i < 5; ++i){
    		buy_items[i] = buy_items_set[i].size();
    		beh_items[i] = beh_items_set[i].size();
    	}
    	for(int i = 0; i < 30; ++i){
    		if(i < 7){
    			if(buy_day_flag[i])
    				buy_days[0] += 1;
    			if(cart_day_flag[i])
    				cart_days[0] += 1;
    			if(beh_day_flag[i])
    				beh_days[0] += 1;
    		}
    		if(i < 15){
    			if(buy_day_flag[i])
    				buy_days[1] += 1;
    			if(cart_day_flag[i])
    				cart_days[1] += 1;
    			if(beh_day_flag[i])
    				beh_days[1] += 1;
    		}
    		if(buy_day_flag[i]){
    			buy_days[2] += 1;
    			buy_gap.add(i);
    		}
    		if(cart_day_flag[i])
    			cart_days[2] += 1;
    		if(beh_day_flag[i])
    			beh_days[2] += 1;
    	}
    	if(buy_items[4] != 0)
    		buy_count_per_item = 1.0 * buy_count[4] / buy_items[4];
    	if(beh_items[4] != 0)
    		beh_count_per_item = 1.0 * beh_count[4] / beh_items[4];
    	if(beh_count[4] != 0)
    		user_buy_count_ratio = 1.0 * buy_count[4] / beh_count[4];
    	if(beh_days[2] != 0)	
    		user_buy_day_ratio = 1.0 * buy_days[2] / beh_days[2];
    	if(beh_items[4] != 0)
    		user_buy_items_ratio = 1.0 * buy_items[4] / beh_items[4];
    	
    	result.set(0, key.getString("user_id"));
    	if(near_buy_time[0] == -1L)
    		near_buy_time[0] = 720L;
    	if(near_cart_time[0] == -1L)
    		near_cart_time[0] = 720L;
    	if(near_contact_time[0] == -1L)
    		near_contact_time[0] = 720L;
    	for(int i = 0; i < 2; ++i){
    		result.set(i + 1, near_buy_time[i]);
    		result.set(i + 3, near_cart_time[i]);
    		result.set(i + 5, near_contact_time[i]);
    	}
    	for(int i = 0; i < 5; ++i){
    		result.set(i + 7, buy_count[i]);
    		result.set(i + 12, cart_count[i]);
    		result.set(i + 17, beh_count[i]);
    		result.set(i + 22, buy_items[i]);
    		result.set(i + 27, cart_items[i]);
    		result.set(i + 32, beh_items[i]);
    	}
    	for(int i = 0; i < 3; ++i){
    		result.set(i + 37, buy_days[i]);
    		result.set(i + 40, cart_days[i]);
    		result.set(i + 43, beh_days[i]);
    	}
    	result.set(46, buy_count_per_item);
    	result.set(47, beh_count_per_item);
    	result.set(48, user_buy_count_ratio);
    	result.set(49, user_buy_day_ratio);
    	result.set(50, user_buy_items_ratio);
    	
    	if (buy_gap.size() <= 1) {
    		buy_frequency = 31.0;
		} else {
			for (int i = 1; i < buy_gap.size(); ++i)
				buy_frequency += 1.0 * ((Integer)buy_gap.get(i) - (Integer)buy_gap.get(i - 1));
			buy_frequency /= (buy_gap.size() - 1);
		}
    	
    	result.set(51, buy_frequency);
    	for(int i = 0; i < 5; ++i)
    		result.set(i + 52, buy_flag[i]);
    	context.write(result);
    }

    public static long getHourSpan(String sj1, String sj2) {
		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd HH");
        long day = 0;
        try {
        	Date date1 = myFormatter.parse(sj1);
        	Date date2 = myFormatter.parse(sj2);
        	day = (date1.getTime() - date2.getTime()) / (60 * 60 * 1000);
        } catch (Exception e) {
        	return -1;
        }
        return day;
	}
    
    public void cleanup(TaskContext arg0) throws IOException {

    }
}