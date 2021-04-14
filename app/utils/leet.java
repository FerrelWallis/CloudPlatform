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

    //动态规划空间优化 一维dp[tlen] 由 dp[slen][tlen]
    //因此仅与上一层 j 和上一层 j - 1有关 如果
    public int numDistinct3(String s, String t) {
        int slen = s.length(), tlen = t.length();
        int[] dp = new int[tlen + 1];
        dp[0] = 1;
        for(int i = 1; i <= slen; i++) {
            for(int j = tlen; j > 0; j--) {
                if(s.charAt(i - 1) == t.charAt(j - 1)) dp[j] += dp[j - 1];
            }
        }
        return dp[tlen];
    }


    //动态规划14ms
    //子问题：两个字符串的动态规划问题常规做法，建立二维dp
    //       bab 与 b的子集数量 和 ba 与 b的子集数量 以及 ba 与 "" 的数量有关
    //状态定义：dp[tlen][slen] 当 s[j] == t[i] 时，即 ba(b) 碰到了 a(b), 要考虑两种情况：
    //         1. 两边都没有b的时候子集个数 2. s没有b的时候的自己个数（即前面还有没有b的子集）
    //         s[j] != t[i], 即 abdc 碰到 ab, 只需看 abd 与ab之间子集数量
    //dp方程：if(s.charat(j - 1) == t.charat(i - 1)) dp[i][j] = dp[i - 1][j - 1] + dp[i][j - 1]
    //       else dp[i][j] = dp[i][j - 1]
    public int numDistinct(String s, String t) {
        int slen = s.length(), tlen = t.length();
        int[][] dp = new int[tlen + 1][slen + 1];
        for(int i = 0; i <= slen; i++) dp[0][i] = 1;
        for(int i = 1; i <= tlen; i++) {
            for(int j = 1; j <= slen; j++) {
                if(s.charAt(j - 1) == t.charAt(i - 1)) dp[i][j] = dp[i - 1][j - 1] + dp[i][j - 1];
                else dp[i][j] = dp[i][j - 1];
            }
        }
        return dp[tlen][slen];
    }

    //i、j互换
    public int numDistinct2(String s, String t) {
        int slen = s.length(), tlen = t.length();
        int[][] dp = new int[slen + 1][tlen + 1];
        for(int i = 0; i <= slen; i++) dp[i][0] = 1;
        for(int i = 1; i <= slen; i++) {
            for(int j = 1; j <= tlen; j++) {
                if(s.charAt(j - 1) == t.charAt(i - 1)) dp[i][j] = dp[i - 1][j - 1] + dp[i - 1][j];
                else dp[i][j] = dp[i - 1][j];
            }
        }
        return dp[slen][tlen];
    }



    //DistinctSubsequencesII_940
    //Given a string S, count the number of distinct, non-empty subsequences of S. Since the result may be
    //large, return the answer modulo 10^9 + 7.

    //Example 1:
    //Input: "abc"
    //Output: 7
    //Explanation: The 7 distinct subsequences are "a", "b", "c", "ab", "ac", "bc", and "abc".

    //Example 2:
    //Input: "aba"
    //Output: 6
    //Explanation: The 6 distinct subsequences are "a", "b", "ab", "ba", "aa" and "aba".

    //Example 3:
    //Input: "aaa"
    //Output: 3
    //Explanation: The 3 distinct subsequences are "a", "aa" and "aaa".

    //Note:
    //S contains only lowercase letters.
    //1 <= S.length <= 2000



    //动态规划
    //子问题：首先子集的定义是字母间相对顺序不变，因此越是在右边的元素，以那元素为起点的子集就越少。最后一个元素的子集就是其本身。
    //       找 b子集个数和 ab子集个数之间的关系。 b: b  ab: a + ab + b
    //       找 ab子集 和 cab子集之间的关系。
    //              ab: a + ab + b
    //              cab: c + ab + cab(这里cab表示的是c跟所有ab子集的组合ca、cab、cb，因此cab数量 = ab数量)
    //状态定义：由上面两个子问题可以看出，假设原字符串S的子集数量是 s，每当往字符串S的前面添加一个元素 x，
    //         如果x第一次出现，xS的子集的数量 = 1 + S字符串的所有子集数量 + x对S所有子集的组合（==S子集数量），即 1 + 2 * S子集数量
    //         如果x不是第一次出现，xS子集的数量 = 0 + 2 * S子集数量 - (前面所有以x为起始的字符串的子集index - 1)的子集数量,
    //         例如 b(bgb) = 0 + 2 * bgb数量 - gb数量
    //dp方程：定义dp[i]表示以i为起点的字符串的子集数量
    //       dp[i] = (s[i]第一次出现? 1 : 0) + dp[i + 1] * 2 - (第一次s[i]出现)？0 ：sum(dp[x]) x表示前面所有s[i]字符出现的下标
    public static void main(String[] args) {
        leet test = new leet();
        System.out.println(test.distinctSubseqII("ccc"));
    }

    public int distinctSubseqII(String S) {
        char[] s = S.toCharArray();
        int len = s.length;
        int[] pre = new int[26]; //记录上一个重复字符出现的index
        Arrays.fill(pre, len);
        int[] dp = new int[len];
        dp[len - 1] = 1;
        pre[s[len - 1] - 'a'] = len - 1;
        for(int i = len - 2; i >= 0; i--) {
            int minus = 0, preindex = pre[s[i] - 'a'];
            if(preindex < len - 1) minus = dp[preindex + 1];
            dp[i] = ((pre[s[i] - 'a'] == len)? 1 : 0) + 2 * dp[i + 1] - minus;
            pre[s[i] - 'a'] = i;
        }
        return dp[0];
    }





    //RaceCar_818
    //Your car starts at position 0 and speed +1 on an infinite number line.  (Your car can go into negative
    //positions.) Your car drives automatically according to a sequence of instructions A (accelerate) and R
    //(reverse). When you get an instruction "A", your car does the following: position += speed, speed *= 2.
    //When you get an instruction "R", your car does the following: if your speed is positive then speed = -1 ,
    //otherwise speed = 1.  (Your position stays the same.) For example, after commands "AAR", your car goes to
    //positions 0->1->3->3, and your speed goes to 1->2->4->-1. Now for some target position, say the length of
    //the shortest sequence of instructions to get there.

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

    //Note: 1 <= target <= 10000.

    //子问题: 再不reverse的情况下, 1A 3AA 7AAA 15AAAA 31AAAAA....,拿5为例，他的最小数量之可能存在与2种情况，(7-2)或(3-1+3)
    //状态定义: 即所有数字可以分为2种情况：1.只有A就可以获得(log2(t) + 1) 2.需要R才能获得
    //         而第2种情况中有分两种情况,假设x为最近大于target的无A的操作数，pos1为最近大于target的无A的位置（操作数为x），
    //         pos2为最远小于target的无A的位置(操作数为x-1)
    //         1. 走到 pos1，再往回走 t - pos1，总操作数为 x + dp[t - pos1] + 1(1是R)
    //         2. 走到 pos2，往回走 back = ((1 << b) - 1) (操作数b:1~x-2)，再往前走 t - pos2 + back, 总操作数为 (x - 1) + b + dp[t - pos2 + back]
    //dp方程: dp[i] 表示走到 i 这个位置的操作数 dp[1] = 1 dp[2] = 4
    //x = log2(i) + 1 表示最近大于target的无A操作数；pos = (1 << x) - 1 表示最近大于target的无A的所在位置
    // if(pos == i) dp[i] = x
    // else { dp[i] = x + dp[pos - t] + 1
    // for(int x1 = 1; x1 < x - 1; x1++) { dp[i] = min(dp[i], x - 1 + ) }
    // }
    public int racecar2(int target) {
        int speed = 1, position = 0, control;
        while (position < target) {
            position += speed;
        }
        return 0;
    }

    class Solution {
        public int racecar(int target) {
            int[] f = new int[target + 2];
            f[1]=1; //A
            f[2]=4; //AARA 或者 ARRA
            int k = 2;
            // S记录连续k个A指令，达到的位置
            int S = 3;
            for (int i = 3; i <= target; i++) {
                if(i == S) {
                    f[i] = k++;
                    // 2^k - 1
                    S = (1<<k) - 1;
                } else {
                    // 情况1：连续k个A后，回退
                    f[i] = k + 1 + f[S-i];
                    // 情况2：连续k-1个A后，回退(0/1/.../k-2)步后，再前进
                    for (int back = 0; back <= k-2; back++) {
                        // 回退后还需前进的距离：i+S(back)-S(k-1)
                        int distance = i + (1<<back) - (1<<(k-1));
                        f[i] = Math.min(f[i], (k-1)+2+back+f[distance]);
                    }
                }
            }
            return f[target];
        }
    }



    int[] dp = new int[10001];
    public int racecar(int t) {
        if (dp[t] > 0) return dp[t];
        int n = (int)(Math.log(t) / Math.log(2)) + 1;
        if (1 << n == t + 1) { //
            dp[t] = n;
        } else {
            dp[t] = racecar((1 << n) - 1 - t) + n + 1;
            for (int m = 0; m < n - 1; ++m) {
                dp[t] = Math.min(dp[t], racecar(t - (1 << (n - 1)) + (1 << m)) + n + m + 1);
            }
        }
        return dp[t];
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
