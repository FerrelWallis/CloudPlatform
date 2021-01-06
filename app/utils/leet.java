package utils;

import com.google.inject.internal.asm.$ModuleVisitor;
import com.google.inject.internal.cglib.proxy.$Factory;
import com.google.inject.internal.cglib.proxy.$InvocationHandler;
import javafx.util.Pair;
import scala.Char;
import scala.Predef;
import scala.concurrent.java8.FuturesConvertersImpl;
import sun.reflect.generics.tree.Tree;

import java.util.*;

public class leet {

    public static void main(String[] args) {
        leet test = new leet();
        int[] nums = new int[]{1,2,3,1};
        //[2,1,3,null,4]   7
        String a = "()(()";

        char aa = '1';
        char bb = '0';
        String conbine = bb+""+aa;
        int cc = Integer.parseInt(conbine);
        System.out.println(cc);
    }


    //BurstBalloons_312
    //You are given n balloons, indexed from 0 to n - 1. Each balloon is painted with a number on it
    //represented by an array nums. You are asked to burst all the balloons.
    //If you burst the ith balloon, you will get nums[left] * nums[i] * nums[right] coins. Here left and
    //right are adjacent indices of i. After the burst, the left and right then becomes adjacent.
    //Return the maximum coins you can collect by bursting the balloons wisely.

    //Example 1:
    //Input: nums = [3,1,5,8]
    //Output: 167
    //Explanation:
    //nums = [3,1,5,8] --> [3,5,8] --> [3,8] --> [8] --> []
    //coins =  3*1*5    +   3*5*8   +  1*3*8  + 1*8*1 = 167

    //Example 2:
    //Input: nums = [1,5]
    //Output: 10
    //动态规划 子问题：遍历范围内所有的气球，将当前气球作为最后一个气球戳爆时，能获取的最大值。 最后求所有作为最后一个气球求出来的值中的最大值
    //               例如 [1,2,3,4,5] 假设遍历到 3 所求的就是 当3作为最后一个戳爆的气球，能获取的最大值。
    //               最后求就是所有气球里面最作为后一个戳爆时能获取到的最大值中最大的数
    //状态定义： left right边界范围内，
    //气球作为最后一个被戳破 = max(气球左侧所有气球能获取到的最大值) + max(气球右侧所有气球能获取到的最大值) + 当前nums当前气球 * nums（左边界 - 1） * nums（右边界 + 1）
    //dp方程： left ~ right范围内： dp[left][right] = max(val[i])   i : left ~ right
    //        val[i] = max(left ~ i-1) + max(i+1 ~ right) + nums[i] * nums[left-1] * nums[right+1]
    //时间复杂度：O(n^3)，其中 n 是气球数量。区间数为 n^2，区间迭代复杂度为 O(n)，最终复杂度为 O(n^2×n)=O(n^3)
    //空间复杂度：O(n^2)，其中 n 是气球数量。缓存大小为区间的个数。
    public int maxCoins(int[] nums) {
        int[][] dp = new int[nums.length][nums.length];
        return maxCoins(nums, 0, nums.length - 1, dp);
    }

    private int maxCoins(int[] nums, int left, int right, int[][] dp) {
        if(left > right) return 0;
        if(dp[left][right] != 0) return dp[left][right];
        for(int i = left; i <= right; i++) {
            int maxleft = maxCoins(nums, left, i - 1, dp);
            int maxright = maxCoins(nums, i + 1, right, dp);
            int leftedge = 1, rightedge = 1;
            if(left - 1 >= 0) leftedge = nums[left - 1];
            if(right + 1 < nums.length) rightedge = nums[right + 1];
            int lastblast = nums[i] * leftedge * rightedge;
            dp[left][right] = Math.max(dp[left][right], maxleft + maxright + lastblast);
        }
        return dp[left][right];
    }


    //动态规划 依旧是自顶向下 转变思考方式(实际与上一个一样) 从气球序列里戳气球，改成往两边界为1的区间里填充气球，获取气球的累加值
    //状态定义： addBalloon(i,j)表示充满ij区间能获取到的最大值 枚举 ij边界中间的空隙mid  addBalloon(i,j) = max(addBalloon(i,mid) + addBalloon(mid,j) + i * j * nums[mid])
    //dp方程：addBalloon(i,j) = max(addBalloon(i, mid) + addBalloon(mid, j) + i * j * nums[mid]) mid: i+1 ~ j-1 if(i < j-1) 表明ij之间有空隙填充气球的情况下
    //       addBalloon(i,j) = 0  if(i >= j-1) 表面ij之间不能添加气球
    //时间复杂度：O(n^3)，其中 n 是气球数量。区间数为 n^2，区间迭代复杂度为 O(n)，最终复杂度为 O(n^2×n)=O(n^3)
    //空间复杂度：O(n^2)，其中 n 是气球数量。缓存大小为区间的个数。
    public int maxCoins2(int[] nums) {
        int[][] dp = new int[nums.length + 2][nums.length + 2];
        return addBalloon(nums,-1, nums.length, dp);
    }

    private int addBalloon(int[] nums, int i, int j, int[][] dp) {
        if(dp[i+1][j+1] != 0) return dp[i+1][j+1];
        if(i >= j - 1) return 0;
        int left = i, right = j;
        if(i == -1) left = 1;
        if(j == nums.length) right = 1;
        for(int mid = i + 1; mid < j; mid++) {
            int sum = left * right * nums[mid] + addBalloon(nums, i, mid, dp) + addBalloon(nums, mid, j, dp);
            dp[i+1][j+1] = Math.max(sum, dp[i+1][j+1]);
        }
        return dp[i+1][j+1];
    }


    //动态规划 自底向上 改变dp访问的顺序
    //因为  addBalloon(i,j) = max(addBalloon(i, mid) + addBalloon(mid, j) + i * j * nums[mid])
    //也就是 dp[i][j] = max(dp[i][mid] + dp[mid][j] + i * j * nums[mid]) mid: i ~ j  if(i > j - 1)
    //时间复杂度：O(n^3)，其中 n 是气球数量。区间数为 n^2，区间迭代复杂度为 O(n)，最终复杂度为 O(n^2×n)=O(n^3)
    //空间复杂度：O(n^2)，其中 n 是气球数量。缓存大小为区间的个数。
    public int maxCoins3(int[] nums) {
        int[] val = new int[nums.length + 2];
        val[0] = 1; val[nums.length + 1] = 1;
        for(int i = 0; i < nums.length; i++) val[i+1] = nums[i];
        int[][] dp = new int[nums.length + 2][nums.length + 2];
        for(int i = val.length - 3; i >= 0; i--) {
            for(int j = i + 2; j < val.length; j++) {
                for(int mid = i + 1; mid < j; mid++) {
                    dp[i][j] = Math.max(dp[i][j], dp[i][mid] + dp[mid][j] + val[i] * val[j] * val[mid]);
                }
            }
        }
        return dp[0][nums.length + 1];
    }


    //MaximalSquare_221
    //Given an m x n binary matrix filled with 0's and 1's, find the largest square containing only 1's and
    //return its area.

    //Example 1:
    //Input: matrix = [["1","0","1","0","0"],["1","0","1","1","1"],["1","1","1","1","1"],["1","0","0","1","0"]]
    //Output: 4

    //Example 2:
    //Input: matrix = [["0","1"],["1","0"]]
    //Output: 1

    //Example 3:
    //Input: matrix = [["0"]]
    //Output: 0

    //Constraints:
    //m == matrix.length
    //n == matrix[i].length
    //1 <= m, n <= 300
    //matrix[i][j] is '0' or '1'.

    //动态规划：子问题：最大正方形面积，把当前访问到的位置作为正方形右下角（前提是当前为1），如果当前位置的左、上、左上都为1 则长宽扩大
    //状态定义：定义一个spread(i,j)方法 查看当前能否进一步扩展成更大的正方形  matrix[i-1][j]、matrix[i][j-1]、matrix[i-1][j-1]
    //         如果前 matrix[i][j]为1，则可以作为正方形右下角，基础面积为1.
    //dp方程：dp[i][j]表示以 i,j为右下角能构成的最大正方形面积
    public int maximalSquare(char[][] matrix) {



        return 0;
    }












    //MaxSumofRectangleNoLargerThanK_363
    //Given a non-empty 2D matrix matrix and an integer k, find the max sum of a rectangle in the matrix such that its sum is no larger than k.
    //
    //Example:
    //
    //Input: matrix = [[1,0,1],[0,-2,3]], k = 2
    //Output: 2
    //Explanation: Because the sum of rectangle [[0, 1], [-2, 3]] is 2,
    //             and 2 is the max number no larger than k (k = 2).
    //Note:
    //
    //The rectangle inside the matrix must have an area > 0.
    //What if the number of rows is much larger than the number of columns?
    public int maxSumSubmatrix(int[][] matrix, int k) {
        return 0;
    }


    //MinimumWindowSubstring_76
    //Given two strings s and t, return the minimum window in s which will contain all the characters in t. If there is no such window in s that covers all characters in t, return the empty string "".
    //
    //Note that If there is such a window, it is guaranteed that there will always be only one unique minimum window in s.
    //
    // 
    //
    //Example 1:
    //
    //Input: s = "ADOBECODEBANC", t = "ABC"
    //Output: "BANC"
    //Example 2:
    //
    //Input: s = "a", t = "a"
    //Output: "a"
    // 
    //
    //Constraints:
    //
    //1 <= s.length, t.length <= 105
    //s and t consist of English letters.
    // 
    //
    //Follow up: Could you find an algorithm that runs in O(n) time?
    public String minWindow(String s, String t) {
        return "";
    }



    //PalindromicSubstrings_647
    //Given a string, your task is to count how many palindromic substrings in this string.
    //The substrings with different start indexes or end indexes are counted as different substrings
    //even they consist of same characters.
    //
    //Example 1:
    //Input: "abc"
    //Output: 3
    //Explanation: Three palindromic strings: "a", "b", "c".
    //
    //Example 2:
    //Input: "aaa"
    //Output: 6
    //Explanation: Six palindromic strings: "a", "a", "a", "aa", "aa", "aaa".
    //
    //Note:The input string length won't exceed 1000.

    //假设一个回文串中心为 center，该中心对应的最大回文串右边界为 right。存在一个 i 为当前回文串中心，满足 i > center，那么也存在一个 j 与 i 关于 center 对称，可以根据 Z[i] 快速计算出 Z[j]。
    //
    //当 i < right 时，找出 i 关于 center 的对称点 j = 2 * center - i。此时以 i 为中心，半径为 right - i 的区间内存在的最大回文串的半径 Z[i] 等于 Z[j]。
    //
    //例如，对于字符串 A = '@#A#B#A#A#B#A#＄'，当 center = 7, right = 13, i = 10 时，center 为两个字母 A 中间的 #，最大回文串右边界为最后一个 #，i 是最后一个 B，j 是第一个 B。
    //
    //在 [center - (right - center), right] 中，区间中心为 center，右边界为 right，i 和 j 关于 center 对称，且 Z[j] = 3，可以快速计算出 Z[i] = min(right - i, Z[j]) = 3。
    //
    //在 while 循环中，只有当 Z[i] 超过 right - i 时，才需要逐个比较字符。这种情况下，Z[i] 每增加 1，right 也会增加 1，且最多能够增加 2*N+2 次。因此这个过程是线性的。
    //
    //最后，对 Z 中每一项 v 计算 (v+1) / 2，然后求和。假设给定最大回文串中心为 C，半径为 R，那么以 C 为中心，半径为 R-1, R-2, ..., 0 的子串也都是回文串。例如 abcdedcba 是以 e 为中心，半径为 4 的回文串，那么 e，ded，cdedc，bcdedcb 和 abcdedcba 也都是回文串。
    //
    //除以 2 是因为实际回文串的半径为 v 的一半。例如回文串 a#b#c#d#e#d#c#b#a 的半径为实际原回文串半径的 2 倍。

    //1 manacher算法
    public int countSubstrings(String s) {
        return 0;
    }



    //2 中心扩散法3ms



    //3 动态规划


    //SplitArrayLargestSum_410
    //Given an array nums which consists of non-negative integers and an integer m, you can split the array into m non-empty continuous subarrays.
    //
    //Write an algorithm to minimize the largest sum among these m subarrays.
    //
    // 
    //
    //Example 1:
    //
    //Input: nums = [7,2,5,10,8], m = 2
    //Output: 18
    //Explanation:
    //There are four ways to split nums into two subarrays.
    //The best way is to split it into [7,2,5] and [10,8],
    //where the largest sum among the two subarrays is only 18.
    //Example 2:
    //
    //Input: nums = [1,2,3,4,5], m = 2
    //Output: 9
    //Example 3:
    //
    //Input: nums = [1,4,4], m = 3
    //Output: 4
    public int splitArray(int[] nums, int m) {
        return 0;
    }


    //StudentAttendanceRecordI_551
    //You are given a string representing an attendance record for a student. The record only contains the following three characters:
    //'A' : Absent.
    //'L' : Late.
    //'P' : Present.
    //A student could be rewarded if his attendance record doesn't contain more than one 'A' (absent) or more than two continuous 'L' (late).
    //
    //You need to return whether the student could be rewarded according to his attendance record.
    //
    //Example 1:
    //Input: "PPALLP"
    //Output: True
    //Example 2:
    //Input: "PPALLL"
    //Output: False
    public boolean checkRecord(String s) {
        return false;
    }


    //StudentAttendanceRecordII_552
    //Given a positive integer n, return the number of all possible attendance records with length n, which will be regarded as rewardable. The answer may be very large, return it after mod 109 + 7.
    //
    //A student attendance record is a string that only contains the following three characters:
    //
    //'A' : Absent.
    //'L' : Late.
    //'P' : Present.
    //A record is regarded as rewardable if it doesn't contain more than one 'A' (absent) or more than two continuous 'L' (late).
    //
    //Example 1:
    //Input: n = 2
    //Output: 8
    //Explanation:
    //There are 8 records with length 2 will be regarded as rewardable:
    //"PP" , "AP", "PA", "LP", "PL", "AL", "LA", "LL"
    //Only "AA" won't be regarded as rewardable owing to more than one absent times.
    //Note: The value of n won't exceed 100,000.
    public int checkRecord(int n) {
        return 0;
    }


    //TaskScheduler_621
    //Given a characters array tasks, representing the tasks a CPU needs to do, where each letter represents a different task. Tasks could be done in any order. Each task is done in one unit of time. For each unit of time, the CPU could complete either one task or just be idle.
    //
    //However, there is a non-negative integer n that represents the cooldown period between two same tasks (the same letter in the array), that is that there must be at least n units of time between any two same tasks.
    //
    //Return the least number of units of times that the CPU will take to finish all the given tasks.
    //
    // 
    //
    //Example 1:
    //
    //Input: tasks = ["A","A","A","B","B","B"], n = 2
    //Output: 8
    //Explanation:
    //A -> B -> idle -> A -> B -> idle -> A -> B
    //There is at least 2 units of time between any two same tasks.
    //Example 2:
    //
    //Input: tasks = ["A","A","A","B","B","B"], n = 0
    //Output: 6
    //Explanation: On this case any permutation of size 6 would work since n = 0.
    //["A","A","A","B","B","B"]
    //["A","B","A","B","A","B"]
    //["B","B","B","A","A","A"]
    //...
    //And so on.
    //Example 3:
    //
    //Input: tasks = ["A","A","A","A","A","A","B","C","D","E","F","G"], n = 2
    //Output: 16
    //Explanation:
    //One possible solution is
    //A -> B -> C -> A -> D -> E -> A -> F -> G -> A -> idle -> idle -> A -> idle -> idle -> A
    public int leastInterval(char[] tasks, int n) {
        return 0;
    }




}
