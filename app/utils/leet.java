package utils;

import com.google.protobuf.Internal;
import com.mysql.jdbc.util.Base64Decoder;
import javafx.util.Pair;
import scala.Array;
import scala.collection.mutable.DoubleLinkedList;
import scala.collection.mutable.LinkedHashSet;
import scala.compat.java8.converterImpl.StepsDoubleLinkedHashTableValue;
import sun.awt.ModalExclude;

import java.util.*;

public class leet {



    //84.最大矩形面积
    //动态规划(目前最优) 2ms 时间复杂度O(n) 空间复杂度O(n)
    public static int largestRectangleArea3(int[] height) {
        if (height == null || height.length == 0) {
            return 0;
        }
        //存放左边比它小的下标
        int[] leftLess = new int[height.length];
        //存放右边比它小的下标
        int[] rightLess = new int[height.length];
        rightLess[height.length - 1] = height.length;
        leftLess[0] = -1;

        //动态规划（当前元素的结果可由上一级元素的结果获得），计算每个柱子左边比它小的柱子的下标
        //如果当前元素的左元素小于当前元素，当前的左哨兵为左元素
        //如果左元素大于当前元素，则p为左元素的哨兵，再判断左元素的哨兵是否大于当前元素，大于则p为哨兵的哨兵
        for (int i = 1; i < height.length; i++) {
            int p = i - 1;
            while (p >= 0 && height[p] >= height[i]) {
                p = leftLess[p];
            }
            leftLess[i] = p;
        }
        //计算每个柱子右边比它小的柱子的下标
        for (int i = height.length - 2; i >= 0; i--) {
            int p = i + 1;
            while (p < height.length && height[p] >= height[i]) {
                p = rightLess[p];
            }
            rightLess[i] = p;
        }
        int maxArea = 0;
        //以每个柱子的高度为矩形的高，计算矩形的面积。
        for (int i = 0; i < height.length; i++) {
            maxArea = Math.max(maxArea, height[i] * (rightLess[i] - leftLess[i] - 1));
        }
        return maxArea;
    }

    //动态规划 思路和单调栈一致，用dp替代栈来完成寻找左右边界
    //子问题：当前i的左边界与i-1和i-1的左边界有什么关系
    //状态定义：leftdp[i]记录左侧最近的一个小于height[i]的下标，
    //          如果height[i] > height[i-1] 那么 leftdp[i]=i-1
    //          如果height[i] <= height[i-1] 那么寻找 i-1的左边界leftdp[i-1]，
    //          查看height[i]是否大于height[leftdp[i-1]]，以此类推直到找到比当前小的x leftdp[i] = x
    public static int largestRectangleArea4(int[] height) {
        int len = height.length;
        int[] leftdp = new int[len], rightdp = new int[len];
        leftdp[0] = -1;
        rightdp[len - 1] = len;
        for(int i = 0; i < len; i++) {
            int x = i - 1;
            while (x >= 0 && height[i] < height[x]) {
                x = leftdp[x];
            }

        }
        return 0;
    }


    //单调栈常数优化 时间复杂度O(n) 空间复杂度O(n) 10ms
    //左右边界确定不用两次循环，而是一同在左边界确定的过程中，确定有边界。
    //遍历到i的时候，如果heights[i]小于之前的某个index，需要把index pop出来，
    //这时说明i是index右边第一个小于height[index]的，即i是index的右边界。
    //遍历完成，栈内没被pop的说明有边界是height.length
    public int largestRectangleArea2(int[] heights) {
        if(heights == null || heights.length == 0) return 0;
        int len = heights.length;
        int[] left = new int[len], right = new int[len];
        Arrays.fill(right, len);
        Stack<Integer> stack = new Stack<>();
        for(int i = 0; i < len; i++) {
            if(!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                right[stack.pop()] = i;
            }
            left[i] = stack.isEmpty()? -1 : stack.peek();
            stack.push(i);
        }
        int max = 0;
        for(int i = 0; i < len; i++) {
            max = Math.max(max, heights[i] * (right[i] - left[i] - 1));
        }
        return max;
    }

    //单调栈，三次循环，时间复杂度O(n),空间复杂度O(n) 13ms
    //找以当前heights作为矩形高时，左边能抵达的最远距，即从当前向左找第一个小于当前height的下标，没有就是-1
    //同理找右边边界
    //最后算所有height作为高时的面积取最大
    public int largestRectangleArea(int[] heights) {
        int[] left = new int[heights.length], right = new int[heights.length];
        Stack<Integer> stack = new Stack<>(); //单调栈
        for(int i = 0; i < heights.length; i++) {
            while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                stack.pop();
            }
            left[i] = (stack.isEmpty())? -1 : stack.peek();
            stack.push(i);
        }
        stack.clear();
        for(int i = heights.length - 1; i >= 0; i--) {
            while (!stack.isEmpty() && heights[stack.peek()] >= heights[i]) {
                stack.pop();
            }
            right[i] = (stack.isEmpty())? heights.length : stack.peek();
            stack.push(i);
        }
        int max = 0;
        for(int i = 0; i < heights.length; i++) {
            max = Math.max(max, heights[i] * (right[i] - left[i] - 1));
        }
        return max;
    }


    //MaximalRectangle_85
    //Given a rows x cols binary matrix filled with 0's and 1's, find the largest rectangle containing only 1's and return its area.
    //Example 1:
    //
    //
    //Input: matrix = [["1","0","1","0","0"],["1","0","1","1","1"],["1","1","1","1","1"],["1","0","0","1","0"]]
    //Output: 6
    //Explanation: The maximal rectangle is shown in the above picture.
    //Example 2:
    //
    //Input: matrix = []
    //Output: 0
    //Example 3:
    //
    //Input: matrix = [["0"]]
    //Output: 0
    //Example 4:
    //
    //Input: matrix = [["1"]]
    //Output: 1
    //Example 5:
    //
    //Input: matrix = [["0","0"]]
    //Output: 0
    // 
    //
    //Constraints:
    //
    //rows == matrix.length
    //cols == matrix[i].length
    //0 <= row, cols <= 200
    //matrix[i][j] is '0' or '1'.

    //动态规划
    public int maximalRectangle2(char[][] matrix) {
        int row = matrix.length, col = matrix[0].length, max = 0;
        int[] heights = new int[col];
        for(int i = 0; i < row; i++) {
            for(int j = 0; j < col; j++) {
                if(matrix[i][j] == '1') heights[j]++;
                else heights[j] = 0;
            }
            int[] leftdp = new int[col], rightdp = new int[col];
            leftdp[0] = -1;
            Arrays.fill(rightdp, col);
            for(int j = 1; j < col; j++) {
                int x = j - 1;
                while (x >= 0 && heights[x] >= heights[j]) {
                    x = leftdp[x];
                }
                leftdp[j] = x;
            }
            for(int j = col - 2; j < col; j--) {
                int x = j + 1;
                while (x < col && heights[x] >= heights[j]) {
                    x = rightdp[x];
                }
                rightdp[j] = x;
            }
            for(int j = 0; j < col; j++) {
                max = Math.max(max, heights[j] * (rightdp[j] - leftdp[j] - 1));
            }
        }
        return max;
    }

    //单调栈
    public int maximalRectangle(char[][] matrix) {
        int row = matrix.length, col = matrix[0].length, max = 0;
        int[] height = new int[col];
        for(int i = 0; i < row; i++) {
            for(int j = 0; j < col; j++) {
                if(matrix[i][j] == '1') height[j]++;
                else height[j] = 0;
            }
            int[] left = new int[col], right = new int[col];
            Arrays.fill(right, col);
            Stack<Integer> stack = new Stack<>();
            for(int j = 0; j < col; j++) {
                while (!stack.isEmpty() && height[stack.peek()] >= height[j]){
                    right[stack.pop()] = j;
                }
                left[j] = (stack.isEmpty())? -1 : stack.peek();
                stack.push(j);
            }
            for(int j = 0; j < col; j++) {
                max = Math.max(max, height[j] * (right[j] - left[j] - 1));
            }
        }
        return max;
    }



    //456. 132pattern
    //Given an array of n integers nums, a 132 pattern is a subsequence of three integers nums[i], nums[j]
    //and nums[k] such that i < j < k and nums[i] < nums[k] < nums[j]. Return true if there is a 132 pattern
    //in nums, otherwise, return false.
    //Follow up: The O(n^2) is trivial, could you come up with the O(n logn) or the O(n) solution?

    //Example 1:
    //Input: nums = [1,2,3,4]
    //Output: false
    //Explanation: There is no 132 pattern in the sequence.

    //Example 2:
    //Input: nums = [3,1,4,2]
    //Output: true
    //Explanation: There is a 132 pattern in the sequence: [1, 4, 2].

    //Example 3:
    //Input: nums = [-1,3,2,0]
    //Output: true
    //Explanation: There are three 132 patterns in the sequence: [-1, 3, 2], [-1, 3, 0] and [-1, 2, 0].

    //Constraints:
    //n == nums.length
    //1 <= n <= 104
    //-109 <= nums[i] <= 109

    //特殊3，2，1  1，2，1  1,3,3
    public static void main(String[] args) {
        leet test = new leet();
        System.out.println(test.find132pattern(new int[]{1,2,1,3}));
    }

    //单调栈 实现
    public boolean find132pattern(int[] nums) {
        Stack<Integer> stack = new Stack<>();
        for(int i = 0; i < nums.length; i++) {
            while (!stack.isEmpty() && nums[stack.peek()] >= nums[i]) {
                stack.pop();
            }

            stack.push(i);
        }
        return false;
    }





    //DistinctSubsequences_115
    //Given a string S and a string T, count the number of distinct subsequences of S which equals T.
    //A subsequence of a string is a new string which is formed from the original string by deleting some
    //(can be none) of the characters without disturbing the relative positions of the remaining characters.
    //(ie, "ACE" is a subsequence of "ABCDE" while "AEC" is not). It's guaranteed the answer fits on a
    //32-bit signed integer.

    //Example 1:
    //Input: S = "rabbbit", T = "rabbit"
    //Output: 3
    //Explanation:
    //As shown below, there are 3 ways you can generate "rabbit" from S.
    //(The caret symbol ^ means the chosen letters)
    //rabbbit
    //^^^^ ^^
    //rabbbit
    //^^ ^^^^
    //rabbbit
    //^^^ ^^^

    //Example 2:
    //Input: S = "babgbag", T = "bag"
    //Output: 5
    //Explanation:
    //As shown below, there are 5 ways you can generate "bag" from S.
    //(The caret symbol ^ means the chosen letters)
    //babgbag
    //^^ ^
    //babgbag
    //^^    ^
    //babgbag
    //^    ^^
    //babgbag
    //  ^  ^^
    //babgbag
    //    ^^^

    //优化动态规划6ms
    public int numDistinct2(String s, String t) {
        int[] dp = new int[t.length()+1];
        dp[0] = 1;
        for(int i=0; i < s.length(); i++) {
            for(int j=t.length(); j > 0; j--) {
                if(s.charAt(i) == t.charAt(j-1)) {
                    dp[j] += dp[j-1];
                }
            }
        }
        return dp[t.length()];
    }


    //动态规划14ms
    public int numDistinct(String s, String t) {
        int[][] dp = new int[t.length() + 1][s.length() + 1];
        for (int j = 0; j < s.length() + 1; j++) dp[0][j] = 1;
        for (int i = 1; i < t.length() + 1; i++) {
            for (int j = 1; j < s.length() + 1; j++) {
                if (t.charAt(i - 1) == s.charAt(j - 1))
                    dp[i][j] = dp[i - 1][j - 1] + dp[i][j - 1];
                else dp[i][j] = dp[i][j - 1];
            }
        }
        return dp[t.length()][s.length()];
    }



    //DistinctSubsequencesII_940
    //Given a string S, count the number of distinct, non-empty subsequences of S .
    //
    //Since the result may be large, return the answer modulo 10^9 + 7.
    //
    // 
    //
    //Example 1:
    //
    //Input: "abc"
    //Output: 7
    //Explanation: The 7 distinct subsequences are "a", "b", "c", "ab", "ac", "bc", and "abc".
    //Example 2:
    //
    //Input: "aba"
    //Output: 6
    //Explanation: The 6 distinct subsequences are "a", "b", "ab", "ba", "aa" and "aba".
    //Example 3:
    //
    //Input: "aaa"
    //Output: 3
    //Explanation: The 3 distinct subsequences are "a", "aa" and "aaa".

    //Note:
    //S contains only lowercase letters.
    //1 <= S.length <= 2000
    public int distinctSubseqII(String S) {
        return 0;
    }





    //RaceCar_818
    //Your car starts at position 0 and speed +1 on an infinite number line.  (Your car can go into negative positions.)
    //
    //Your car drives automatically according to a sequence of instructions A (accelerate) and R (reverse).
    //
    //When you get an instruction "A", your car does the following: position += speed, speed *= 2.
    //
    //When you get an instruction "R", your car does the following: if your speed is positive then speed = -1 , otherwise speed = 1.  (Your position stays the same.)
    //
    //For example, after commands "AAR", your car goes to positions 0->1->3->3, and your speed goes to 1->2->4->-1.
    //
    //Now for some target position, say the length of the shortest sequence of instructions to get there.
    //
    //Example 1:
    //Input:
    //target = 3
    //Output: 2
    //Explanation:
    //The shortest instruction sequence is "AA".
    //Your position goes from 0->1->3.
    //Example 2:
    //Input:
    //target = 6
    //Output: 5
    //Explanation:
    //The shortest instruction sequence is "AAARA".
    //Your position goes from 0->1->3->7->7->6.
    // 
    //
    //Note:
    //
    //1 <= target <= 10000.
    public int racecar(int target) {
        return 0;
    }




    //256. Paint House
    //There is a row of n houses, where each house can be painted one of three colors: red, blue, or green. The cost of painting each house with a certain color is different. You have to paint all the houses such that no two adjacent houses have the same color.
    //The cost of painting each house with a certain color is represented by a n x 3 cost matrix. For example, costs[0][0] is the cost of painting house 0 with the color red; costs[1][2] is the cost of painting house 1 with color green, and so on... Find the minimum cost to paint all houses.
    //
    //
    //
    //Example 1:
    //
    //Input: costs = [[17,2,17],[16,16,5],[14,3,19]]
    //Output: 10
    //Explanation: Paint house 0 into blue, paint house 1 into green, paint house 2 into blue.
    //Minimum cost: 2 + 5 + 3 = 10.
    //
    //Example 2:
    //
    //Input: costs = []
    //Output: 0
    //
    //Example 3:
    //
    //Input: costs = [[7,6,2]]
    //Output: 2
    //
    //
    //
    //Constraints:
    //
    //    costs.length == n
    //    costs[i].length == 3
    //    0 <= n <= 100
    //    1 <= costs[i][j] <= 20
    public int minCost(int[][] costs) {
        return 0;
    }


    //265. Paint House II
    //
    //There are a row of n houses, each house can be painted with one of the k colors. The cost of painting each house with a certain color is different. You have to paint all the houses such that no two adjacent houses have the same color.
    //
    //The cost of painting each house with a certain color is represented by a n x k cost matrix. For example, costs[0][0] is the cost of painting house 0 with color 0; costs[1][2] is the cost of painting house 1 with color 2, and so on... Find the minimum cost to paint all houses.
    //
    //Note:
    //All costs are positive integers.
    //
    //Example:
    //
    //Input: [[1,5,3],[2,9,4]]
    //Output: 5
    //Explanation: Paint house 0 into color 0, paint house 1 into color 2. Minimum cost: 1 + 4 = 5;
    //             Or paint house 0 into color 2, paint house 1 into color 0. Minimum cost: 3 + 2 = 5.
    //
    //Follow up:
    //Could you solve it in O(nk) runtime?
    public int minCostII(int[][] costs) {
        return 0;
    }


    //1473. Paint House III
    //There is a row of m houses in a small city, each house must be painted with one of the n colors (labeled from 1 to n), some houses that has been painted last summer should not be painted again.
    //
    //A neighborhood is a maximal group of continuous houses that are painted with the same color. (For example: houses = [1,2,2,3,3,2,1,1] contains 5 neighborhoods  [{1}, {2,2}, {3,3}, {2}, {1,1}]).
    //
    //Given an array houses, an m * n matrix cost and an integer target where:
    //
    //houses[i]: is the color of the house i, 0 if the house is not painted yet.
    //cost[i][j]: is the cost of paint the house i with the color j+1.
    //Return the minimum cost of painting all the remaining houses in such a way that there are exactly target neighborhoods, if not possible return -1.
    //
    //
    //
    //Example 1:
    //
    //Input: houses = [0,0,0,0,0], cost = [[1,10],[10,1],[10,1],[1,10],[5,1]], m = 5, n = 2, target = 3
    //Output: 9
    //Explanation: Paint houses of this way [1,2,2,1,1]
    //This array contains target = 3 neighborhoods, [{1}, {2,2}, {1,1}].
    //Cost of paint all houses (1 + 1 + 1 + 1 + 5) = 9.
    //Example 2:
    //
    //Input: houses = [0,2,1,2,0], cost = [[1,10],[10,1],[10,1],[1,10],[5,1]], m = 5, n = 2, target = 3
    //Output: 11
    //Explanation: Some houses are already painted, Paint the houses of this way [2,2,1,2,2]
    //This array contains target = 3 neighborhoods, [{2,2}, {1}, {2,2}].
    //Cost of paint the first and last house (10 + 1) = 11.
    //Example 3:
    //
    //Input: houses = [0,0,0,0,0], cost = [[1,10],[10,1],[1,10],[10,1],[1,10]], m = 5, n = 2, target = 5
    //Output: 5
    //Example 4:
    //
    //Input: houses = [3,1,2,3], cost = [[1,1,1],[1,1,1],[1,1,1],[1,1,1]], m = 4, n = 3, target = 3
    //Output: -1
    //Explanation: Houses are already painted with a total of 4 neighborhoods [{3},{1},{2},{3}] different of target = 3.
    //
    //
    //Constraints:
    //
    //m == houses.length == cost.length
    //n == cost[i].length
    //1 <= m <= 100
    //1 <= n <= 20
    //1 <= target <= m
    //0 <= houses[i] <= n
    //1 <= cost[i][j] <= 10^4
    public int minCost(int[] houses, int[][] cost, int m, int n, int target) {
        return 0;
    }

}
