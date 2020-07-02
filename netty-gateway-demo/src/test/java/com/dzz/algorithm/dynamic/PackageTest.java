package com.dzz.algorithm.dynamic;

import org.junit.Test;

import java.util.Arrays;

/**
 * @author zoufeng
 * @date 2020-7-1
 * <p>
 * 背包问题变种
 * <p>
 * 金明的预算方案
 * 金明今天很开心，家里购置的新房就要领钥匙了，新房里有一间金明自己专用的很宽敞的房间。
 * 更让他高兴的是，妈妈昨天对他说：“你的房间需要购买哪些物品，怎么布置，你说了算，只要不超过 nn 元钱就行”。
 * 今天一早，金明就开始做预算了，他把想买的物品分为两类：主件与附件，附件是从属于某个主件的，下表就是一些主件与附件的例子：
 * <p>
 * 主件	附件
 * 电脑	打印机，扫描仪
 * 书柜	图书
 * 书桌	台灯，文具
 * 工作椅	无
 * <p>
 * 如果要买归类为附件的物品，必须先买该附件所属的主件。每个主件可以有 0 个、1 个或 2 个附件。
 * 附件不再有从属于自己的附件。金明想买的东西很多，肯定会超过妈妈限定的 N 元。
 * 于是，他把每件物品规定了一个重要度，分为 5 等：用整数 1-5 表示，第 5 等最重要。
 * 他还从因特网上查到了每件物品的价格（都是10元的整数倍）。
 * 他希望在不超过 N 元（可以等于 N 元）的前提下，使每件物品的价格与重要度的乘积的总和最大。
 * <p>
 * 设第 j 件物品的价格为 v[j]，重要度为 w[j]，共选中了 k 件物品，编号依次为 j1,j2,…,jk，则所求的总和为：
 * <p>
 * 　　v[j1]×w[j1]+v[j2]×w[j2]+ …+v[jk]×w[jk]
 * <p>
 * 　　请你帮助金明设计一个满足要求的购物单。
 * <p>
 * <p>
 * <p>
 * <p>
 * 二、分组的背包问题
 * 有 N 组物品和一个容量是 V 的背包。
 * 每组物品有若干个，同一组内的物品最多只能选一个。
 * 每件物品的体积是 w,价值是v
 * 求解将哪些物品装入背包，可使物品总体积不超过背包容量，且总价值最大。
 * 输出最大价值。
 * <p>
 * 分析：
 * 依旧以01背包问题思路，
 * 假设选取第i件物品时，本质是选取特定组的物品，而一个组有s个物品，则该组有s+1种选择：选0个～选s个
 * 从里面选取最优解
 * dp[g][j]=max{dp[g-1][j],max{dp[g-1][j-w[i]]+v[i] for s, 1 < i < s}}
 * <p>
 * 空间优化
 * 和01背包一致，倒序遍历
 * for i 0...i
 * for j c...w[i]
 * d[j]=max{d[j],d[j-w[s]]+v[s]}
 */
public class PackageTest {

    //分组背包问题
    @Test
    public void groupPackage() {
        int group = 4; //分组物品编号

        int[][] weight = { //分组物体积
                {3, 2, 3, 4},
                {1, 2, 3, 4},
                {1, 2, 1, 4},
                {1, 2, 3, 4}
        };

        int[][] value = { //分组物品价值
                {1, 2, 3, 7},
                {1, 5, 3, 4},
                {2, 2, 3, 4},
                {1, 6, 3, 4}
        };
        int c = 10;//限制重量
        int[][] dp = new int[group + 1][c + 1];

        // d[j]=max{d[j],d[j-w[s]]+v[s]}
        for (int g = 1; g < group + 1; g++) {
            for (int j = 0; j < c + 1; j++) {
                //dp[g][j]=max{dp[g-1][j],max{dp[g-1][j-w[i]]+v[i] for s, 1 < i < s}}
                int maxGi = 0;
                for (int k = 0; k < weight[g - 1].length && j >= weight[g - 1][k]; k++) {
                    maxGi = Math.max(maxGi, dp[g - 1][j - weight[g - 1][k]] + value[g - 1][k]);
                }
                dp[g][j] = Math.max(dp[g - 1][j], maxGi);
            }
        }


        for (int i = 0; i < dp.length; i++) {
            for (int j = 0; j < dp[0].length; j++) {
                System.out.print(dp[i][j] + " ");
            }
            System.out.println();
        }

    }


    //分组背包空间优化
    @Test
    public void groupLimitedSpace() {
        int group = 4;//分组物品编号
        int[][] weight = { //分组物体积
                {3, 2, 3, 4},
                {1, 2, 3, 4},
                {1, 2, 1, 4},
                {1, 2, 3, 4}
        };
        int[][] value = { //分组物品价值
                {1, 2, 3, 7},
                {1, 5, 3, 4},
                {2, 2, 3, 4},
                {1, 6, 3, 4}
        };
        int c = 10;//限制重量
        int[] dp = new int[c + 1];

        for (int g = 1; g < group + 1; g++) {
            for (int j = c; j > 0; j--) {
                int maxGi = 0;
                for (int k = 0; k < weight[g - 1].length && j >= weight[g - 1][k]; k++) {
                    maxGi = Math.max(maxGi, dp[j - weight[g - 1][k]] + value[g - 1][k]);
                }
                dp[j] = Math.max(dp[j], maxGi);
            }
        }

        for (int i : dp) {
            System.out.print(i + " ");
        }

    }

    /*
     * 从M件物品中取K件物品，得到价值最大值
     * 限制重量为c
     *
     * 这个和01背包的问题的区别是忽略了放入物品的顺序
     * 01背包是限制重量的情况下获取前i种物品放入时最大的值，没有限定物品种类的值
     * 相当于01背包问题多加了一个限制，变成了一个二维背包问题
     *
     * 问题
     * 二维费用的背包问题是指：对于每件物品，具有两种不同的费用；选择这件物品必须同时付出这两种代价；对于每种代价都有一个可付出的最大值（背包容量）。
     * 问怎样选择物品可以得到最大的价值。设这两种代价分别为代价1和代价2，第i件物品所需的两种代价分别为a[i]和b[i]。
     * 两种代价可付出的最大值（两种背包容量）分别为V和U。 物品的价值为w[i]。
     *
     * 算法
     * 费用加了一维，只需状态也加一维即可。设f[i][v][u]表示前i件物品付出两种代价分别为v和u时可获得的最大价值。
     * 状态转移方程就是：
     *  f [i][v][u]=max{f[i-1][v][u],f[i-1][v-a[i]][u-b[i]]+w[i]}。
     * 如前述方法，可以只使用二维的数组：当每件物品只可以取一次时变量v和u采用逆序的循环，
     * 当物品有如完全背包问题时采用顺序的循环。当物品有如多重背包问题时拆分物品。
     *
     * 物品总个数的限制
     * 有时，“二维费用”的条件是以这样一种隐含的方式给出的：
     * 最多只能取M件物品。这事实上相当于每件物品多了一种“件数”的费用，
     * 每个物品的件数费用均为1，可以付出的最大件数费用为M。
     * 换句话说，设f[v][m]表示付出费用v、最多选m件时可得到的最大价值，
     * 则根据物品的类型（01、完全、多重）用不同的方法循环更新，最后在f[0..V][0..M]范围内寻找答案。
     * 另外，如果要求“恰取M件物品”，则在f[0..V][M]范围内寻找答案。
     *
     * 所以这个问题的状态转移方程是：
     * dp[i][v][u]=max{dp[i-1][v][u],dp[i-1][v-a[i]][u-b[i]]+w[i]}。
     *
     * */
    @Test
    public void testTakeKFromM() {
        int[] weight = {2, 5, 1, 4, 7};
        int[] value = {2, 2, 6, 4, 5};
        int c = 10;//限制重量
        int n = 2;//限制数量
        int[][][] dp = new int[weight.length + 1][c + 1][3];

        //dp[i][j][k]=max{dp[i-1][j][k],dp[i-1][j-w[i]][k-1]+v[i]}
        for (int i = 1; i < weight.length + 1; i++) {
            for (int j = weight[i - 1]; j < c + 1; j++) {
                for (int k = 1; k < n + 1; k++) {
                    dp[i][j][k] = Math.max(dp[i - 1][j][k], dp[i - 1][j - weight[i - 1]][k - 1] + value[i - 1]);
                }
            }
        }

        for (int i = 0; i < weight.length + 1; i++) {
            for (int j = 0; j < c + 1; j++) {
                System.out.print(dp[i][j][2] + " ");
            }
            System.out.println();
        }
        System.out.println();
        System.out.println("================");
        for (int i = 0; i < weight.length + 1; i++) {
            for (int j = 0; j < c + 1; j++) {
                System.out.print(dp[i][j][1] + " ");
            }
            System.out.println();
        }


        int k = 2;
        System.out.println("===========");
        for (int i = weight.length; i > 0 && k > 0; ) {
            for (int j = c; j > 0 && k > 0; ) {
                if (dp[i][j][k] != dp[i - 1][j][k]) {
                    System.out.println("放入了第 " + i + " 件物品");
                    j -= weight[i - 1];
                    k--;
                }
                //没有放入第i件物品
                i--;
            }
            System.out.println();
        }


    }

    //二维背包空间优化版
    @Test
    public void testTakeKFromMLimitedSpace() {
        int[] weight = {2, 5, 1, 4, 3};
        int[] value = {2, 2, 1, 4, 5};
        int c = 10;//限制重量
        int n = 2;//限制数量
        int[][] dp = takeKFromMByLimitedSpace(weight, value, c, n);

        for (int i = 0; i < c + 1; i++) {
            System.out.print(dp[i][2] + " ");
        }
        System.out.println();
        for (int i = 0; i < c + 1; i++) {
            System.out.print(dp[i][1] + " ");
        }
    }

    private int[][] takeKFromMByLimitedSpace(int[] weight, int[] value, int c, int n) {
        int[][] dp = new int[c + 1][n + 1];
        //dp[i][j][k]=max{dp[i-1][j][k],dp[i-1][j-w[i]][k-1]+v[i]}
        //dp[j][k]=max{dp[j][k],dp[j-w[i]][k-1]+v[i]}
        for (int i = 1; i < weight.length + 1; i++) {
            for (int j = c; j >= weight[i - 1]; j--) {
                for (int k = n; k > 0; k--) {//思考为什么也要倒序
                    dp[j][k] = Math.max(dp[j][k], dp[j - weight[i - 1]][k - 1] + value[i - 1]);
                }
            }
        }
        return dp;
    }


    /*
     * 金明的预算方案
     * 物品分为主件和附件
     * 主件数量为i,附件数量0-M
     * 物品花费为n ，重要度V
     * 限制花费为N
     *
     * 求（花费*重要度）最大值
     *
     * 分析
     * 思路一：
     * 可以抽象成分组背包问题，花费是重量w，重要度是价值v
     * 将物品主件和附件划分为同一组，背包放入第i件主件即求第i组的最大值
     * 为了方便计算，如果分组内不足M件，则补足为M件，不过补足的部分花费和重要度都是0
     * 分组计算的状态转移方程为：
     *  dp[g][j]=max{dp[g-1][j],max{dp[g-1][j-w[i]]+w[i]*v[i] for s, 1 < i < s}}
     * 这里第二项相当于是在M个物品中取K [0,M]件物品，得到花费和重要度乘积最大值，相当又一个背包子问题问题
     * sdp[j]=max{sdp[j],sdp[j-w[i]+w[i]*v[i]]}
     * 这种思路有一个问题是需要得到遍历过程中重量和对应的值
     *
     * 思路二、
     * 01背包是放入第i件物品是考虑放或不放两种情况，
     * 这里是放入第i件组件，有什么都不放，只放主件，只放一个附件，放两附件五种情况
     * v[i][0]v[i][0]表示第i个物品的主件价值，
     *  v[i][1]v[i][1]表示第i个物品的第一个附件的价值，
     * v[i][2]v[i][2]表示第i个物品的第二个附件的价值。
     * w[i][0..2]w[i][0..2]表示同样的物品的体积。
     * f[i,j]f[i,j]表示给定i个物品和j的空间能够获得的最大价值总合。
     * 则：
     * f[i,j]=max{f[i−1,j],f[i,j]=max{f[i−1,j],
     * f[i−1,j−w[i,0]]+v[i,0],f[i−1,j−w[i,0]]+v[i,0],
     * f[i−1,j−w[i,0]w[i,1]]+v[i,0]+v[i,1],f[i−1,j−w[i,0]w[i,1]]+v[i,0]+v[i,1],
     * f[i−1,j−w[i,0]−w[i,2]]+v[i,0]+v[i,2],f[i−1,j−w[i,0]−w[i,2]]+v[i,0]+v[i,2],
     * f[i−1,j−w[i,0]−w[i,1]−w[i,2]]+v[i,0]+v[i,1]+v[i,2]}
     *
     *
     * 思路三、
     * 通过引入“物品组”和“依赖”的概念可以加深对这题的理解，还可以解决它的推广问题。
     * 用物品组的思想考虑那题中极其特殊的依赖关系：
     * 物品不能既作主件又作附件，每个主件最多有两个附件，
     * 可以发现一个主件和它的两个附件等价于一个由四个物品组成的物品组，这便揭示了问题的某种本质。
     *
     * */
    @Test
    public void budget() {
        int group = 4;
        int[][] weight = { //分组物体积
                {3, 2, 3},
                {3, 2, 3},
                {4, 2, 1},
                {5, 2, 3}
        };

        int[][] value = { //分组物品价值
                {1, 2, 3},
                {1, 5, 3},
                {2, 2, 3},
                {1, 6, 3}
        };
        int c = 10;//限制重量
        int[][] dp = new int[group + 1][c + 1];

        for (int g = 1; g < group + 1; g++) {
            for (int j = 0; j < c + 1; j++) {
                int w0 = weight[g - 1][0];
                int w1 = weight[g - 1][1];
                int w2 = weight[g - 1][2];
                int v0 = value[g - 1][0];
                int v1 = value[g - 1][1];
                int v2 = value[g - 1][2];
                dp[g][j] = maxMore(
                        dp[g - 1][j]
                        , j >= w0 ? dp[g - 1][j - w0] + w0 * v0 : 0
                        , j >= w0 + w1 ? dp[g - 1][j - w0 - w1] + w0 * v0 + w1 * v1 : 0
                        , j >= w0 + w2 ? dp[g - 1][j - w0 - w2] + w0 * v0 + w2 * v2 : 0
                        , j >= w0 + w1 + w2 ? dp[g - 1][j - w0 - w1 - w2] + w0 * v0 + w1 * v1 + w2 * v2 : 0
                );
            }
        }

        for (int i = 0; i < dp.length; i++) {
            for (int j = 0; j < dp[0].length; j++) {
                System.out.print(dp[i][j] + " ");
            }
            System.out.println();
        }

        //反推表达式可获取放入的物品
/*        for (int i = dp.length - 1; i > 0; ) {
            for (int j = dp[0].length - 1; j > 0; ) {
                if (dp[i][j] != dp[i - 1][j]) {
                    System.out.println("放入了第 " + i + " 件");

                    i--;
                }
                j--;
            }
            System.out.println();
        }*/
    }


    private int maxMore(int... array) {
        int max = 0;
        for (int j = 0; j < array.length; j++) {
            max = Math.max(max, array[j]);
        }
//        System.out.println(max);
        return max;
    }
}
