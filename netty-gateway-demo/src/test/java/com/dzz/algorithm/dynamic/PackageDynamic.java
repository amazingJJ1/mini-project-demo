package com.dzz.algorithm.dynamic;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2020-6-30
 * <p>
 * 动态规划：
 * 本质是暴力枚举
 */
public class PackageDynamic {

    //背包问题
    // 01背包问题描述：有编号分别为a,b,c,d,e的五件物品，它们的重量分别是2,2,6,5,4，
    // 它们的价值分别是6,3,5,4,6，每件物品数量只有一个，现在给你个承重为10的背包，
    // 如何让背包里装入的物品具有最大的价值总和？

    /**
     * 一、01背包问题
     * 思路：
     * 暴力枚举后的计算复杂度为O（2^n）,这个看起来不可接受
     * <p>
     * 可以采用动态规划将复杂度减少到O（NM）
     * dp[i][j]表示将前i件物品装进限重为j的背包可以获得的最大价值, 0<=i<=N, 0<=j<=W
     * 那么我们可以将dp[0][0...W]初始化为0，表示将前0个物品（即没有物品）装入书包的最大价值为0。
     * 那么当 i > 0 时dp[i][j]有两种情况：不装入第i件物品，即dp[i−1][j]；装入第i件物品（前提是能装下），即dp[i−1][j−w[i]] + v[i]。
     * 即状态转移方程为：
     * 即dp[i][j] = max(dp[i−1][j], dp[i−1][j−w[i]]+v[i]) // j >= w[i]
     * 由上述状态转移方程可知，dp[i][j]的值只与dp[i-1][0,...,j-1]有关，所以我们可以采用动态规划常用的方法（滚动数组）对空间进行优化（即去掉dp的第一维）。
     * 需要注意的是，为了防止上一层循环的dp[0,...,j-1]被覆盖，循环的时候 j 只能逆向枚举（空间优化前没有这个限制），
     * 伪代码为：// 01背包问题伪代码(空间优化版)
     * dp[0,...,W] = 0
     * for i = 1,...,N
     * for j = W,...,w[i] // 必须逆向枚举!!!
     * dp[j] = max(dp[j], dp[j−w[i]]+v[i])
     *
     * <p>
     * 动态规划的核心思想避免重复计算在01背包问题中体现得淋漓尽致。
     * 第i件物品装入或者不装入而获得的最大价值完全可以由前面i-1件物品的最大价值决定，暴力枚举忽略了这个事实。
     * <p>
     * <p>
     * <p>
     * 二、无限背包 问题
     * 和01背包不同的地方就是放入背包的物品可以重复放入
     * <p>
     * 无限背包思路一
     * 我们的目标和变量和01背包没有区别，所以我们可定义与01背包问题几乎完全相同的状态
     * dp:dp[i][j]表示将前i种物品装进限重为j的背包可以获得的最大价值, 0<=i<=N, 0<=j<=W
     * 初始状态也是一样的，我们将dp[0][0...W]初始化为0，
     * 表示将前0种物品（即没有物品）装入书包的最大价值为0。
     * 那么当 i > 0 时dp[i][j]也有两种情况：
     * 1.不装入第i种物品，即dp[i−1][j]，同01背包；
     * 2.装入第i种物品，此时和01背包不太一样，因为每种物品有无限个（但注意书包限重是有限的），
     * 所以此时不应该转移到dp[i−1][j−w[i]]而应该转移到dp[i][j−w[i]]，
     * 即装入第i种商品后还可以再继续装入第种商品。
     * 所以状态转移方程为dp[i][j] = max(dp[i−1][j], dp[i][j−w[i]]+v[i]) // j >= w[i]
     * 这个状态转移方程与01背包问题唯一不同就是max第二项不是dp[i-1]而是dp[i]。
     * 和01背包问题类似，也可进行空间优化，
     * 优化后不同点在于这里的 j 只能正向枚举而01背包只能逆向枚举，
     * 【因为这里的max第二项是dp[i]而01背包是dp[i-1]，即这里就是需要覆盖，而01背包需要避免覆盖。】
     * 所以伪代码如下：// 完全背包问题思路一伪代码(空间优化版)
     * dp[0,...,W] = 0
     * for i = 1,...,N
     * for j = w[i],...,W // 必须正向枚举!!!
     * dp[j] = max(dp[j], dp[j−w[i]]+v[i])
     * 由上述伪代码看出，01背包和完全背包问题此解法的空间优化版解法唯一不同就是前者的 j 只能逆向枚举而后者的 j 只能正向枚举，
     * 这是由二者的状态转移方程决定的。此解法时间复杂度为O(NW), 空间复杂度为O(W)。
     * <p>
     * 完全背包思路二：
     * 除了分析一的思路外，完全背包还有一种常见的思路，但是复杂度高一些。
     * 我们从装入第 i 种物品多少件出发，01背包只有两种情况即取0件和取1件，而这里是取0件、1件、2件...直到超过限重（k > j/w[i]），
     * 所以状态转移方程为：
     * # k为装入第i种物品的件数, k <= j/w[i]
     * dp[i][j] = max{(dp[i-1][j − k*w[i]] + k*v[i]) for every k}
     * 同理也可以进行空间优化，需要注意的是，这里max里面是dp[i-1]，和01背包一样，所以 j 必须逆向枚举，优化后伪代码为// 完全背包问题思路二伪代码(空间优化版)
     * dp[0,...,W] = 0
     * for i = 1,...,N
     * for j = W,...,w[i] // 必须逆向枚举!!!
     * for k = [0, 1,..., j/w[i]]
     * dp[j] = max(dp[j], dp[j−k*w[i]]+k*v[i])
     * 相比于思路一，此种方法不是在O(1)时间求得dp[i][j]，所以总的时间复杂度就比分析一大些了，为 O（NM(W/w)）级别。
     * <p>
     * <p>
     * 三、多重背包问题
     * 这个是无限背包的限制，限定了同一物品放入只能N个
     * 这个和无限背包思路二的过程差不多，只不过是在放入第i中物品时多了一个限制N
     * # k为装入第i种物品的件数, k <= min{j/w[i],N}
     * dp[i][j] = max{(dp[i-1][j − k*w[i]] + k*v[i]) for every k}
     * <p>
     * 四、其他问题，背包问题变种
     * 1.恰好装满
     * 背包问题有时候还有一个限制就是必须恰好装满背包，此时基本思路没有区别，
     * 只是在初始化的时候有所不同。如果没有恰好装满背包的限制，我们将dp全部初始化成0就可以了。
     * 因为任何容量的背包都有一个合法解“什么都不装”，这个解的价值为0，所以初始时状态的值也就全部为0了。
     * 如果有恰好装满的限制，那只应该将dp[0,...,N][0]初始为0，其它dp值均初始化为-inf，
     * 因为此时只有容量为0的背包可以在什么也不装情况下被“恰好装满”，
     * 其它容量的背包初始均没有合法的解，应该被初始化为-inf。
     * <p>
     * <p>
     * 我们来证明下无效状态无法推出有效状态。
     * 对于下面的状态转移方程
     * <p>
     * f[i][j] = max(f[i - 1][j], f[i - 1][j - w[i]] + v[i]);
     * （1）如果f[i - 1][j]和 f[i - 1][j - w[i]] 都是有效状态，那么f[i][j]是由有效状态推出的。
     * <p>
     * （2）如果f[i - 1][j]是无效状态， f[i - 1][j - w[i]] 是有效状态：
     * 此时f[i][j]背包容量为j，f[i - 1][j - w[i]] 背包容量为j - w[i]且是恰好装满的。
     * 从f[i - 1][j - w[i]] 变换到f[i][j]相当于向一个已经装了总体积为j - w[i]物体的背包中放入一个体积为w[i]的物体，新的容量为j与当前背包容量j相同！！！
     * 刚才我们说了无效状态可以看作负无穷，于是 f[i - 1][j - w[i]] + v[i]作为有效状态一定大于f[i - 1][j]，所以可以得到新的有效状态f[i][j]。
     * 所以此时f[i][j]是有效状态且是由有效状态f[i - 1][j - w[i]]推出的。
     * <p>
     * （3）如果f[i - 1][j]是有效状态， f[i - 1][j - w[i]] 是无效状态：
     * f[i - 1][j - w[i]] 是无效状态说明当前的背包内装的物体体积小于j - w[i]，再放入一个体积为w[i]的物体后容量小于j，也就是说f[i - 1][j - w[i]] + v[i]也是无效状态。
     * 此时因为f[i - 1][j]是有效状态，所以一定大于无效状态f[i - 1][j - w[i]] + v[i]，则f[i][j]是一个由效状态f[i - 1][j]推导出的有效状态。
     * 所以此时f[i][j]是有效状态且是由有效状态f[i - 1][j]推出的。
     * <p>
     * 【综上，只有有效状态可以推出有效状态。】
     * <p>
     * 2、方案总数
     * 除了在给定每个物品的价值后求可得到的最大价值外，
     * 还有一类问题是问装满背包或将背包装至某一指定容量的方案总数。
     * 对于这类问题，需要将状态转移方程中的 max 改成 sum ，初始条件dp[0][0]=1，大体思路是不变的。
     * 例如若每件物品均是完全背包中的物品，转移方程即为dp[i][j] = sum(dp[i−1][j], dp[i][j−w[i]]) // j >= w[i]
     *
     * 作者：SMON
     * 链接：https://zhuanlan.zhihu.com/p/93857890
     * 来源：知乎
     * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
     * <p>
     * <p>
     * <p>
     * <p>
     * 五、引申问题
     * 1、分割等和子集
     * 题目给定一个只包含正整数的非空数组。问是否可以将这个数组分割成两个子集，使得两个子集的元素和相等。
     * 由于所有元素的和sum已知，所以两个子集的和都应该是sum/2（所以前提是sum不能是奇数），
     * 即题目转换成从这个数组里面选取一些元素使这些元素和为sum/2。
     * 如果我们将所有元素的值看做是物品的重量，每件物品价值都为1，所以这就是一个恰好装满的01背包问题。
     * 我们定义空间优化后的状态数组dp，由于是恰好装满，所以应该将dp[0]初始化为0而将其他全部初始化为INT_MIN，
     * 然后按照类似1.2节的伪代码更新dp：int capacity = sum / 2;
     * vector<int>dp(capacity + 1, INT_MIN);
     * dp[0] = 0;
     * for(int i = 1; i <= n; i++)
     * for(int j = capacity; j >= nums[i-1]; j--)
     * dp[j] = max(dp[j], 1 + dp[j - nums[i-1]]);
     * 更新完毕后，如果dp[sum/2]大于0说明满足题意。
     * <p>
     * 2、零钱兑换
     * 题目给定一个价值amount和一些面值，假设每个面值的硬币数都是无限的，问我们最少能用几个硬币组成给定的价值。
     * 如果我们将面值看作是物品，面值金额看成是物品的重量，每件物品的价值均为1，这样此题就是是一个恰好装满的完全背包问题了。
     * 不过这里不是求最多装入多少物品而是求最少，
     * 我们只需要将2.2节的转态转移方程中的max改成min即可，又由于是恰好装满，所以除了dp[0]，其他都应初始化为INT_MAX。
     * <p>
     * 3、目标和
     * 这道题给了我们一个数组（元素非负），和一个目标值，要求给数组中每个数字前添加正号或负号所组成的表达式结果与目标值S相等，
     * <p>
     * 求有多少种情况。假设所有元素和为sum，所有添加正号的元素的和为A，所有添加负号的元素和为B，
     * 则有sum = A + B 且 S = A - B，解方程得A = (sum + S)/2。
     * 即题目转换成：从数组中选取一些元素使和恰好为(sum + S) / 2。
     * 可见这是一个恰好装满的01背包问题，要求所有方案数，将1.2节状态转移方程中的max改成求和即可。
     * 需要注意的是，虽然这里是恰好装满，但是dp初始值不应该是inf，因为这里求的不是总价值而是方案数，
     * 应该全部初始为0（除了dp[0]初始化为1）。
     * int findTargetSumWays(vector<int>& nums, int S) {
     *     int sum = 0;
     *     // for(int &num: nums) sum += num;
     *     sum = accumulate(nums.begin(), nums.end(), 0);
     *     if(S > sum || sum < -S) return 0; // 肯定不行
     *     if((S + sum) & 1) return 0; // 奇数
     *     int target = (S + sum) >> 1;
     *
     *     vector<int>dp(target + 1, 0);
     *
     *     dp[0] = 1;
     *     for(int i = 1; i <= nums.size(); i++)
     *         for(int j = target; j >= nums[i-1]; j--)
     *             dp[j] = dp[j] + dp[j - nums[i-1]];
     *
     *     return dp[target];
     * }
     *
     * <p>
     * <p>
     * 1和0
     * 题目给定一个仅包含 0 和 1 字符串的数组。
     * 任务是从数组中选取尽可能多的字符串，使这些字符串包含的0和1的数目分别不超过m和n。
     * 我们把每个字符串看做是一件物品，把字符串中0的数目和1的数目看做是两种“重量”，
     * 所以就变成了一个二维01背包问题，书包的两个限重分别是 m 和 n，
     * 要求书包能装下的物品的最大数目（也相当于价值最大，设每个物品价值为1）。
     *
     */
    private int[] weight = {2, 2, 6, 5, 4};
    private int[] value = {6, 3, 5, 4, 6};
    private int c = 10;//背包容量

    //二维数组方式
    @Test
    public void package01() {
        int n = 5; //物品总类数
        int[][] dp = new int[n + 1][c + 1]; //f[i][j]表示前i个物品能装入容量为j的背包中的最大价值
        int[][] path = new int[n + 1][c + 1];

        //通过公式迭代计算
        //dp[i][j] = max(dp[i−1][j], dp[i−1][j−w[i]]+v[i]) // j >= w[i]
        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < c + 1; j++) {//重量
                if (weight[i - 1] > j)
                    dp[i][j] = dp[i - 1][j];
                else {
                    if (dp[i - 1][j] < dp[i - 1][j - weight[i - 1]] + value[i - 1]) {
                        dp[i][j] = dp[i - 1][j - weight[i - 1]] + value[i - 1];
                        path[i][j] = 1; //记录放入的物品
                    } else {
                        dp[i][j] = dp[i - 1][j];
                    }
                }
            }
        }
        for (int[] ints : dp) {
            for (int j = 0; j < dp[0].length; j++) {
                System.out.print(ints[j] + " ");
            }
            System.out.println();
        }

        int i = dp.length - 1;
        int j = dp[0].length - 1;
        while (i > 0 && j > 0) {
            if (path[i][j] == 1) {
                System.out.print("第" + i + "个物品装入 ");
                j -= weight[i - 1];
            }
            i--;
        }
    }

    /**
     * 01背包问题伪代码(空间优化版)
     */
    @Test
    public void package01bySpaceLimit() {
        int n = value.length; //物品总类数
        int[] dp = new int[c + 1];

        for (int i = 0; i < n; i++) {
            for (int j = c; j >= weight[i]; j--) {
                //dp[i][j] = Math.max(dp[i-1][j], dp[i-1][j-weight[i-1]]+val[i-1]);
                // dp[i][j]的值只与dp[i-1][0,...,j-1]有关 即dp[j]依赖上个循环的dp[0....,j-1]
                //所以为了防止覆盖，只能逆向枚举
                dp[j] = Math.max(dp[j], dp[j - weight[i]] + value[i]);
            }
        }

        for (int i = 0; i < c + 1; i++) {
            System.out.print(dp[i] + " ");
        }
        System.out.println();
        System.out.println("最大价值为" + dp[c]);
    }

    //无限背包 思路一 二维数组
    @Test
    public void packageUnlimited2() {
        int n = value.length; //物品总类数
        int[][] dp = new int[n + 1][c + 1];

        for (int i = 1; i < n + 1; i++) {
            for (int j = weight[i - 1]; j < (c + 1); j++) {
                dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - weight[i - 1]] + value[i - 1]);
            }
        }

        for (int[] ints : dp) {
            for (int j = 0; j < dp[0].length; j++) {
                System.out.print(ints[j] + " ");
            }
            System.out.println();
        }
    }


    //无限背包 思路一 空间优化版
    @Test
    public void packageUnlimited() {
        int n = value.length; //物品总类数
        int[] dp = new int[c + 1];

        for (int i = 0; i < n; i++) {
            for (int j = weight[i]; j < c + 1; j++) {
                // dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - weight[i - 1]] + value[i - 1]);
                //因为第二个值是dp[i],表示dp[j]即依赖上个循环的dp[j]也依赖本循环的dp[0....,j-1]
                //所以必须正向循环
                dp[j] = Math.max(dp[j], dp[j - weight[i]] + value[i]);
            }
        }

        for (int i = 0; i < c + 1; i++) {
            System.out.print(dp[i] + " ");
        }
        System.out.println();
        System.out.println("最大价值为" + dp[c]);
    }

    //无限背包、思路二、二维数组
    //dp[i][j] = max{(dp[i-1][j − k*w[i]] + k*v[i]) for every k}
    @Test
    public void packageUnlimitedThink2() {
        int n = value.length; //物品总类数
        int[][] dp = new int[n + 1][c + 1];

        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < c + 1; j++) {
                for (int k = 0; j >= (k * weight[i - 1]) && k <= (j / weight[i - 1]); k++) {
                    dp[i][j] = Math.max(dp[i][j], dp[i - 1][j - (k * weight[i - 1])] + k * value[i - 1]);
                }
            }
        }

        for (int[] ints : dp) {
            for (int j = 0; j < dp[0].length; j++) {
                System.out.print(ints[j] + " ");
            }
            System.out.println();
        }
    }

    //无限背包、思路二、空间优化，一维数据
    //dp[i][j] = max{(dp[i-1][j − k*w[i]] + k*v[i]) for every k}
    //dp[j]=max{dp[j − k*w[i]] + k*v[i]}
    //可以看到dp[i][j]依赖dp[i-1][0....j-1]
    //所以为了不覆盖上一轮的数据，必须倒序遍历
    @Test
    public void packageUnlimitedThink2ToSpace() {

        int n = value.length; //物品总类数
        int[] dp = new int[c + 1];
        for (int i = 1; i < n + 1; i++) {
            for (int j = c; j > weight[i - 1]; j--) {
                for (int k = 0; j >= (k * weight[i - 1]) && k <= (j / weight[i - 1]); k++) {
                    dp[j] = Math.max(dp[j], dp[j - (k * weight[i - 1])] + k * value[i - 1]);
                }
            }
        }

        for (int i = 0; i < c + 1; i++) {
            System.out.print(dp[i] + " ");
        }
        System.out.println();
        System.out.println("最大价值为" + dp[c]);
    }

    //恰好装满
    @Test
    public void packageMatchFullLimit() {
        int n = value.length; //物品总类数
        int[][] dp = new int[n + 1][c + 1];
        for (int i = 0; i < n + 1; i++) {
            for (int j = 0; j < c + 1; j++) {
                if (j != 0) {
                    dp[i][j] = -1000;
                }
            }
        }

        for (int i = 1; i < n + 1; i++) {
            for (int j = weight[i - 1]; j < (c + 1); j++) {
                dp[i][j] = Math.max(dp[i - 1][j], dp[i - 1][j - weight[i - 1]] + value[i - 1]);
            }
        }

        for (int[] ints : dp) {
            for (int j = 0; j < dp[0].length; j++) {
                System.out.print(ints[j] + " ");
            }
            System.out.println();
        }
    }

    //恰好装满 空间优化
    @Test
    public void packageMatchFullLimitedSpace() {
        int n = value.length; //物品总类数
        int[] dp = new int[c + 1];
        for (int i = 0; i < c + 1; i++) {
            if (i != 0)
                dp[i] = -1000;
        }

        for (int i = 0; i < n; i++) {
            for (int j = c; j >= weight[i]; j--) {
                //dp[i][j] = Math.max(dp[i-1][j], dp[i-1][j-weight[i-1]]+val[i-1]);
                // dp[i][j]的值只与dp[i-1][0,...,j-1]有关 即dp[j]依赖上个循环的dp[0....,j-1]
                //所以为了防止覆盖，只能逆向枚举
                dp[j] = Math.max(dp[j], dp[j - weight[i]] + value[i]);
            }
        }

        for (int i = 0; i < c + 1; i++) {
            System.out.print(dp[i] + " ");
        }
        System.out.println();
        System.out.println("恰好装满" + c + " 最大价值为" + dp[c]);
    }

}
