package com.dzz.thread;

import org.junit.Test;

/**
 * @author zoufeng
 * @date 2019-9-29
 */
public class ManJianTest {

    /**
     * @param unitPrice   商品单价
     * @param nums        商品数量
     * @param totalAmount 实付款总额
     * @return 商品活动后单价
     */
    private double[] actualUnitPrice(double[] unitPrice, int[] nums, double totalAmount) {
        double actualTotalAmount = 0;
        double[] actualUnitPrice = new double[unitPrice.length];
        //活动前的实际总价
        for (int i = 0; i < unitPrice.length; i++) {
            actualTotalAmount += (unitPrice[i] * nums[i]);
        }
        for (int i = 0; i < unitPrice.length; i++) {
            //产品活动后的实际单价
            actualUnitPrice[i] = (unitPrice[i] * totalAmount) / actualTotalAmount;
        }
        return actualUnitPrice;
    }

    @Test
    public void test() {
        double[] unitPrice = {200, 100, 100};
        int[] num = {1, 2, 1};
        //假设a,b,c的单价分别为200元*1个，100元*2个，100元*1个，满500-50，实际a,b,c所花金额为180*1，90*2，90*1
        double[] calculation = actualUnitPrice(unitPrice, num, 450);
        for (double c : calculation) {
            System.out.println(c);
        }
        //假设a,b,c的单价分别为200元*1个，100元*2个，100元*1个，满500+50（加价购买），实际a,b,c所花金额为220*1，110*2，110*1
        double[] calculation2 = actualUnitPrice(unitPrice, num, 550);
        for (double c : calculation2) {
            System.out.println(c);
        }
    }
}
