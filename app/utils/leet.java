package utils;

import com.google.inject.internal.cglib.proxy.$Factory;
import javafx.util.Pair;
import scala.Char;
import scala.Predef;
import scala.concurrent.java8.FuturesConvertersImpl;

import java.util.*;

public class leet {

    public static void main(String[] args) {
//        leet test=new leet();
//        boolean[] bool=new boolean[10];
//        System.out.println(bool[0]);

    }

    //动态

    //62
    //A robot is located at the top-left corner of a m x n grid (marked 'Start' in the diagram below).
    //
    //The robot can only move either down or right at any point in time. The robot is trying to reach the bottom-right corner of the grid (marked 'Finish' in the diagram below).
    //
    //How many possible unique paths are there?

    //Example 1:
    //
    //Input: m = 3, n = 7
    //Output: 28
    //
    //Example 2:
    //
    //Input: m = 3, n = 2
    //Output: 3
    //Explanation:
    //From the top-left corner, there are a total of 3 ways to reach the bottom-right corner:
    //1. Right -> Down -> Down
    //2. Down -> Down -> Right
    //3. Down -> Right -> Down
    //
    //Example 3:
    //
    //Input: m = 7, n = 3
    //Output: 28
    //
    //Example 4:
    //
    //Input: m = 3, n = 3
    //Output: 6

    //1分治
    public int uniquePaths1(int m, int n) {
        return 0;
    }


    //2自顶向下动态规划
    public int uniquePaths2(int m, int n) {
        return 0;
    }

    //3自底向上动态规划
    public int uniquePaths3(int m, int n) {
        return 0;
    }


    //63
    //A robot is located at the top-left corner of a m x n grid (marked 'Start' in the diagram below).
    //
    //The robot can only move either down or right at any point in time. The robot is trying to reach the bottom-right corner of the grid (marked 'Finish' in the diagram below).
    //
    //Now consider if some obstacles are added to the grids. How many unique paths would there be?
    //
    //An obstacle and space is marked as 1 and 0 respectively in the grid.
    //
    //
    //
    //Example 1:
    //
    //Input: obstacleGrid = [[0,0,0],[0,1,0],[0,0,0]]
    //Output: 2
    //Explanation: There is one obstacle in the middle of the 3x3 grid above.
    //There are two ways to reach the bottom-right corner:
    //1. Right -> Right -> Down -> Down
    //2. Down -> Down -> Right -> Right
    //
    //Example 2:
    //
    //Input: obstacleGrid = [[0,1],[0,0]]
    //Output: 1
    public int uniquePaths4(int m, int n) {
        return 0;
    }





    //方法三：搜索空间的缩减
    //我们可以将已排序的二维矩阵划分为四个子矩阵，其中两个可能包含目标，其中两个肯定不包含。
    //算法：由于该算法是递归操作的，因此可以通过它的基本情况和递归情况的正确性来判断它的正确性。
    //基本情况 ：
    //对于已排序的二维数组，有两种方法可以确定一个任意元素目标是否可以用常数时间判断。
    //第一，如果数组的区域为零，则它不包含元素，因此不能包含目标。
    //其次，如果目标小于数组的最小值或大于数组的最大值，那么矩阵肯定不包含目标值。
    //递归情况：
    //如果目标值包含在数组内，因此我们沿着索引行的矩阵中间列，matrix[row−1][mid]<target<matrix[row][mid]，
    //（很明显，如果我们找到target，我们立即返回true）。现有的矩阵可以围绕这个索引分为四个子矩阵；
    //左上和右下子矩阵不能包含目标（通过基本情况部分来判断），所以我们可以从搜索空间中删除它们 。
    //另外，左下角和右上角的子矩阵是二维矩阵，因此我们可以递归地将此算法应用于它们。

    //时间复杂度：O(nlgn)
    //空间复杂度：O(lgn)，虽然这种方法从根本上不需要大于常量的附加内存，但是它使用递归意味着它将使用与其递归树高度成比例的内存。
    //因为这种方法丢弃了每一级递归一半的矩阵（并进行了两次递归调用），所以树的高度由lgn限定。

    class Solution {
        private int[][] matrix;
        private int target;

        private boolean searchRec(int left, int up, int right, int down) {
            // this submatrix has no height or no width.
            if (left > right || up > down) {
                return false;
                // `target` is already larger than the largest element or smaller
                // than the smallest element in this submatrix.
            } else if (target < matrix[up][left] || target > matrix[down][right]) {
                return false;
            }

            int mid = left + (right-left)/2;

            // Locate `row` such that matrix[row-1][mid] < target < matrix[row][mid]
            int row = up;
            while (row <= down && matrix[row][mid] <= target) {
                if (matrix[row][mid] == target) {
                    return true;
                }
                row++;
            }

            return searchRec(left, row, mid-1, down) || searchRec(mid+1, up, right, row-1);
        }

        public boolean searchMatrix(int[][] mat, int targ) {
            // cache input values in object to avoid passing them unnecessarily
            // to `searchRec`
            matrix = mat;
            target = targ;

            // an empty matrix obviously does not contain `target`
            if (matrix == null || matrix.length == 0) {
                return false;
            }

            return searchRec(0, 0, matrix[0].length-1, matrix.length-1);
        }
    }





}
