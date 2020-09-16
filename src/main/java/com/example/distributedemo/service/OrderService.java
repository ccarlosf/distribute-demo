package com.example.distributedemo.service;

import com.example.distributedemo.dao.OrderItemMapper;
import com.example.distributedemo.dao.OrderMapper;
import com.example.distributedemo.dao.ProductMapper;
import com.example.distributedemo.model.Order;
import com.example.distributedemo.model.OrderItem;
import com.example.distributedemo.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private ProductMapper productMapper;
    //购买商品id
    private int purchaseProductId = 100100;
    //购买商品数量
    private int purchaseProductNum = 1;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    @Autowired
    private TransactionDefinition transactionDefinition;

    private Object object = new Object();

    //    @Transactional(rollbackFor = Exception.class)
    public Integer createOrder() throws Exception {
        Product product = null;

        synchronized (this) {
            TransactionStatus transaction1 = platformTransactionManager.getTransaction(transactionDefinition);

            product = productMapper.selectByPrimaryKey(purchaseProductId);
            if (product == null) {
                platformTransactionManager.rollback(transaction1);
                throw new Exception("购买商品：" + purchaseProductId + "不存在");
            }

            //商品当前库存
            Integer currentCount = product.getCount();
            System.out.println(Thread.currentThread().getName() + "库存数：" + currentCount);
            //校验库存
            if (purchaseProductNum > currentCount) {
                platformTransactionManager.rollback(transaction1);
                throw
                        new Exception("商品" + purchaseProductId + "仅剩" + currentCount + "件，无法购买");

            }

            // 计算剩余库存
//        Integer leftCount = currentCount - purchaseProductNum;
//        // 更新库存
//        product.setCount(leftCount);
//        product.setUpdateTime(new Date());
//        product.setUpdateUser("xxx");
//        productMapper.updateByPrimaryKeySelective(product);

            productMapper.updateProductCount(purchaseProductNum, "xxx", new Date(), product.getId());
            // 检索商品的库存
            // 如果商品库存为负数，抛出异常
            platformTransactionManager.commit(transaction1);

        }

        TransactionStatus transaction = platformTransactionManager.getTransaction(transactionDefinition);

        Order order = new Order();
        order.setOrderAmount(product.getPrice().multiply(new BigDecimal(purchaseProductNum)));
        order.setOrderStatus(1);//待处理
        order.setReceiverName("xxx");
        order.setReceiverMobile("13311112222");
        order.setCreateTime(new Date());
        order.setCreateUser("xxx");
        order.setUpdateTime(new Date());
        order.setUpdateUser("xxx");
        orderMapper.insertSelective(order);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(order.getId());
        orderItem.setProductId(product.getId());
        orderItem.setPurchasePrice(product.getPrice());
        orderItem.setPurchaseNum(purchaseProductNum);
        orderItem.setCreateUser("xxx");
        orderItem.setCreateTime(new Date());
        orderItem.setUpdateTime(new Date());
        orderItem.setUpdateUser("xxx");
        orderItemMapper.insertSelective(orderItem);
        platformTransactionManager.commit(transaction);
        return order.getId();
    }
}
