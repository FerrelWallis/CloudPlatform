package utils;

import com.google.inject.internal.cglib.proxy.$Factory;
import javafx.util.Pair;
import scala.Char;
import scala.Predef;
import scala.concurrent.java8.FuturesConvertersImpl;

import java.util.*;

public class leet {

    public static void main(String[] args) {

    }

    //53. Maximum Subarray
    //Given an integer array nums, find the contiguous subarray (containing at least one number)
    //which has the largest sum and return its sum.
    //Follow up: If you have figured out the O(n) solution, try coding another solution using the divide
    //and conquer approach, which is more subtle.

    //Example 1:
    //Input: nums = [-2,1,-3,4,-1,2,1,-5,4]
    //Output: 6
    //Explanation: [4,-1,2,1] has the largest sum = 6.

    //Example 2:
    //Input: nums = [1]
    //Output: 1

    //Example 3:
    //Input: nums = [0]
    //Output: 0

    //Example 4:
    //Input: nums = [-1]
    //Output: -1

    //Example 5:
    //Input: nums = [-2147483647]
    //Output: -2147483647

    //动态规划 dp[i] = Math.max(0 + nums[i], dp[i-1] + nums[i])
    public int maxSubArray(int[] nums) {
        int n = nums.length, max = nums[0];
        for(int i = 1; i < n; i++) {
            nums[i] = Math.max(0 + nums[i], nums[i-1] + nums[i]);
            max = Math.max(max, nums[i]);
        }
        return max;
    }


    //152. Maximum Product Subarray
    //Given an integer array nums, find the contiguous subarray within an array
    //(containing at least one number) which has the largest product.

    //Example 1:
    //Input: [2,3,-2,4]
    //Output: 6
    //Explanation: [2,3] has the largest product 6.

    //Example 2:
    //Input: [-2,0,-1]
    //Output: 0
    //Explanation: The result cannot be 2, because [-2,-1] is not a subarray.
    public int maxProduct(int[] nums) {
        return 0;
    }

}
