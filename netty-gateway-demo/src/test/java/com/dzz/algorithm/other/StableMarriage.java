package com.dzz.algorithm.other;

/**
 * @author zoufeng
 * @date 2020-8-10
 * 稳定婚姻算法
 * 和二分图匹配差不多
 * <p>
 * 1962年，美国数学家 David Gale 和 Lloyd Shapley 发明了一种寻找稳定婚姻的策略。
 * 不管男女各有多少人，也不管他们的偏好如何，应用这种策略后总能得到一个稳定的搭配。
 * 换句话说，他们证明了稳定的婚姻搭配 总是存在的。有趣的是，这种策略反映了现实生活中的很多真实情况。
 * <p>
 * 在这种策略中，男孩儿将一轮一轮地去追求他中意的女子，女子可以选择接受或者拒绝他的追求者。
 * 第一轮，每个男孩儿都选择自己名单上排在首位的女孩儿，并向她表白。
 * 此时，一个女孩儿可能面对的情况有三种：没有人跟她表白，只有一个人跟她表白，有不止一个人跟她表白。
 * 在第一种情况下，这个女孩儿什么 都不用做，只需要继续等待；
 * 在第二种情况下，接受那个人的表白，答应暂时和他做情侣；
 * 在第三种情况下，从所有追求者中选择自己最中意的那一位，答应和他暂 时做情侣，并拒绝所有其他追求者。
 * <p>
 * 第一轮结束后，有些男孩儿已经有女朋友了，有些男孩儿仍然是单身。
 * 在第二轮追女行动中，每个单身男孩儿都从所有还没拒绝过他的女孩儿中选出自己最中意的那一个，并向她表白，不管她现在是否是单身。
 * 和第一轮一样，女孩儿们需要从表白者中选择最中意的一位，拒绝其他追求者。
 * 注意，如果这个女孩儿已经 有男朋友了，当她遇到了更好的追求者时，她必须拒绝掉现在的男友，投向新的追求者的怀抱。
 * 这样，一些单身男孩儿将会得到女友，那些已经有了女友的人也可能 重新变成光棍。
 * 在以后的每一轮中，单身男孩儿继续追求列表中的下一个女孩儿，女孩儿则从包括现男友在内的所有追求者中选择最好的一个，并对其他人说不。
 * 这 样一轮一轮地进行下去，直到某个时候所有人都不再单身，下一轮将不会有任何新的表白发生，整个过程自动结束。
 * 此时的婚姻搭配就一定是稳定的了。
 * <p>
 * 应用和意义：
 * Gale-Shapley算法最大的意义就在于，作为一个为这些男女牵线的媒人，你并不需要亲自计算稳定婚姻匹配，甚至根本不需要了解每个人的偏好，
 * 只需要按照这个算法组织一个男女配对活动就可以了。你需要做的仅仅是把算法流程当作游戏规则告诉大家，游戏结束后会自动得到一个大家都满意的婚姻匹配。
 * 整个算法可以简单地描述为：每个人都去做自己想做的事情。
 * 对于男性来说，从最喜欢的女孩儿开始追起是顺理成章的事；
 * 对于女性来说，不断选择最好的男孩儿也正好符合她的利益。因此，大家会自动遵守游戏规则，不用担心有人虚报自己的偏好。
 * <p>
 * <p>
 * 这个算法还有很多有趣的性质。
 * 比如说，大家可能会想，这种男追女女拒男的方案对男性更有利还是对女性更有利呢？
 * 答案是，这种方案对男性更有利。
 * 事实上，稳定婚姻搭配往往不止一种，然而上述算法的结果可以保证，
 * 每一位男性得到的伴侣都是所有可能的稳定婚姻搭配方案中最理想的，同时每一位女性得到的伴侣都是所有可能的稳定婚姻搭配方案中最差的。
 * 【为什么对女性不利呢？】
 * 　　（1） 男性能够获得尽可能好的伴侣，结果是：如果还存在其他的稳定匹配，那么里面任何一个男性的伴侣排名都不会比“男优先”得到的结果更好，
 * 我们说此种情况每个男性获得的是“最好”的伴侣。 【男生可以按照心中的意愿将女生排序，然后一个个追求，直到成功】
 * 　　（2） 女性却只能被动地一步步接近她最爱的目标, 但最后往往达不到，结果是:
 * 如果还存在其他的稳定匹配，那么里面任何一个女性的伴侣排名都不会比“男优先”得到的结果更差，我们说此种情况每个女性获得是“最差”的伴侣。
 * 　　（3） 无论男性们求婚的先后顺序如何，最终得到的是同一个稳定婚姻匹配。
 * 当然，这个算法的实现也可是：“女优先”，同样，得到的此稳定婚姻匹配具有和上面相反的三点性质。
 *
 * 问题：
 */
public class StableMarriage {
}
