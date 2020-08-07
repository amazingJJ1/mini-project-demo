package com.dzz.algorithm.string;

import org.junit.Test;

import java.util.*;

/**
 * @author zoufeng
 * @date 2020-8-6
 * <p>
 * AC自动机
 * ac自动机是构建在trie树的基础上的
 * 相当与 trie+kmp算法
 * <p>
 * 一般应用于多模式匹配
 * 比如舆论控制，需要识别一个文本里面的多个敏感词
 * <p>
 * 举例说明
 * 假设我们的字典包含这些词{a,ab,bab,bc,bca,c,caa}，待匹配的字符串是abccab。先根据词典构建的trie
 * <p>
 * 现在开始从字符串i=0的字符开始匹配，trie从root节点开始a,ab你可以匹配到，但是到c的时候你发现匹配失败了，
 * 这时候正常的话需要将i+=1从下一个字符开始匹配，trie又开始从root匹配。但是这里的问题是，我们已经匹配过ab了，
 * 说明下一个字符肯定是b，我们完全没有必要再从trie的root开始匹配，这时候我们从root下面的b节点开始匹配就好了，
 * 也就是【失配指针】指向的地方。
 * 其实这里我们就发现了，失配指针其实存储了字典中词的一些信息，避免我们每次从头开始匹配，
 * 从这里看的话其实跟KMP还是有共性的，存储子串中的一些信息。
 * <p>
 * 失配指针
 * 进一步的话，其实失配指针代表的是子串之间后缀和前缀之间的关系。
 * 对于字符串bca，它的严格后缀（我理解是不包括自己的后缀）是ca,a,None，前缀是bca,bc,b,None；
 * 对于另外一个字符串caa，它的前缀是caa,ca,c,None，我们发现bca的后缀其实出现在caa的前缀中，
 * 因此bca在匹配下一个字符失败的时候，我们可以跳到caa的第一个a节点上继续匹配下一个字符，
 * 因为ca已经匹配过了。
 * 节点i失配指针指向的节点j代表的意思是到节点i为止的字符串最长的严格后缀等于到节点j为止的字符串，
 * 对于上面的bca的例子，如果trie树中存在ca字符串，那么失配节点指向的就是ca的a节点；
 * 如果trie树中只有a，那么就是a节点了；
 * 如果都不存在，那么就是root节点，我们要从头开始匹配。
 * 注意这里提到的是最长的严格后缀，大家可以想想为什么一定要最长？
 * 因为我们匹配的时候是从左到右一个一个字符匹配的，如果不是最长的话我们就丢失了匹配的信息了。
 * 举例来说有bcacay字符串待匹配，有子串bcacax,cacay，
 * 如果不是最长的话bcaca就可能会指向cacay的第一个ca了，那么就丢掉了匹配到的caca信息，造成匹配失败。
 * <p>
 * 可以看到，ac自动机最核心的地方就是失配指针（和KMP的核心是next数组一样）
 * 那么，如何构建失配指针呢？
 * 首先root节点不管，root节点的孩子肯定都是指向root节点的，因为他们的后缀都是空。
 * 假设我们已经有了节点x的失配指针，那么我们如何构造他们孩子child的失配指针呢？
 * 因为失配指针保证的是最大后缀，因此他肯定保证了x之前的字符都是匹配的。
 * 我们知道x的失配指针指向的是节点x的最大后缀y，
 * 因此我们只要看看节点y的孩子节点中是不是有child节点对应的字符，
 * 如果有的话那很好，child的失配指针就是y的那个孩子；
 * 那如果没有呢，那我们就继续看y节点的失配指针了，
 * 因为他也指向y节点的最大后缀，也保证了跟x字符是匹配的。这样一直下去直到相应的节点，或者直到根节点。
 * <p>
 * 模式匹配
 * 我们从根节点开始查找，如果它的孩子能命中目标串的第1个字符串，那么我们就从这个孩子的孩子中再尝试命中目标串的第2个字符串。
 * 否则，我们就顺着它的失配指针，跳到另一个分支，找其他节点。
 * 如果都没有命中，就从根节点重头再来。
 * 当我们节点存在表示有字符串在它这里结束的标识时（如endCound, isEnd），我们就可以确认这字符串已经命中某一个模式串，将它放到结果集中。
 * 如果这时长字符串还没有到尽头，我们继续收集其他模式串。
 */
public class ACAutomataTest {

    class Node {
        Map<Character, Node> map = new HashMap<>();
        char key;
        boolean finished = false;
        Node fail;

        public char getKey() {
            return key;
        }

        public Node setKey(char key) {
            this.key = key;
            return this;
        }

        public boolean isFinished() {
            return finished;
        }

        public Node setFinished(boolean finished) {
            this.finished = finished;
            return this;
        }

        public Node getFail() {
            return fail;
        }

        public Node setFail(Node fail) {
            this.fail = fail;
            return this;
        }

        public Map<Character, Node> getMap() {
            return map;
        }

        public Node setMap(Map<Character, Node> map) {
            this.map = map;
            return this;
        }
    }

    private Node root = new Node().setKey('0');

    private void buildTrie(String[] strings) {
        for (String string : strings) {
            char[] chars = string.toCharArray();
            Node temp = root;
            for (int i = 0; i < chars.length; i++) {
                Map<Character, Node> map = temp.getMap();
                if (map.get(chars[i]) == null) {
                    Node node = new Node().setKey(chars[i]);
                    if (i == chars.length - 1) node.setFinished(true);//设置结束标识
                    map.put(chars[i], node);
                }
                temp = map.get(chars[i]);//指向当前节点
            }
        }
    }

    private void buildAC() {
        ArrayDeque<Node> deque = new ArrayDeque<>();
        deque.add(root);
        do {
            Node node = deque.removeFirst();
            node.getMap().values().forEach(x -> {
                if (node == root) {
                    x.setFail(root);
                } else {
                    Node fail = node.getFail();
                    fail.getMap().values().forEach(
                            y -> {
                                if (y.getKey() == x.getKey()) {
                                    x.setFail(y);
                                }
                            }
                    );
                    if (x.getFail() == null) x.setFail(root);
                }
                deque.addLast(x);
            });

        } while (!deque.isEmpty());
    }

    private Set<String> match(String text) {
        char[] chars = text.toCharArray();
        Set<String> set = new HashSet<>();

        for (int i = 0; i < chars.length; i++) {
            Node node = root.getMap().get(chars[i]);//先从根节点开始匹配
            if (node == null) continue;

            int j = ++i;//i相当于起始游标
            Node temp = node;
            String tempS = node.key + "";
            while (j < chars.length - 1) {
                if (temp.finished) set.add(tempS);//匹配的一些短词
                Map<Character, Node> nodeMap = temp.getMap();
                if (nodeMap.size() == 0) {
                    //说明已匹配完整的字典字符串
                    set.add(tempS);
                    break;
                }
                Node subNode = nodeMap.get(chars[j]);
                if (subNode != null) {//子节点匹配
                    temp = subNode;
                    j++;
                    tempS += temp.key + "";
                } else {
                    //子节点没有则匹配失效指针
                    temp = temp.getFail();
                    if (temp == root) break;
                }
            }
        }
        return set;
    }

    @Test
    public void test() {
        String[] ss = {"a", "ab", "bab", "bc", "bca", "c", "caa"};
        buildTrie(ss);
        buildAC();
        //匹配的词
        Set<String> set = match("abcacaaab");
        set.forEach(System.out::println);
    }
}
