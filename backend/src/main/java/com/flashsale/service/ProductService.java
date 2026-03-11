package com.flashsale.service;

import com.flashsale.entity.Product;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 查询商品列表
     */
    List<Product> getProductList();

    /**
     * 查询商品详情
     */
    Product getProductDetail(Long productId);
}
