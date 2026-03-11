package com.flashsale.service.impl;

import com.flashsale.common.BusinessException;
import com.flashsale.common.ResultCode;
import com.flashsale.entity.Product;
import com.flashsale.mapper.ProductMapper;
import com.flashsale.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    @Override
    public List<Product> getProductList() {
        return productMapper.selectOnShelfList();
    }

    @Override
    public Product getProductDetail(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }
}
