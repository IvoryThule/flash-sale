package com.flashsale.controller;

import com.flashsale.common.Result;
import com.flashsale.entity.Product;
import com.flashsale.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 商品列表
     * GET /api/product/list
     */
    @GetMapping("/list")
    public Result<List<Product>> list() {
        List<Product> products = productService.getProductList();
        return Result.success(products);
    }

    /**
     * 商品详情
     * GET /api/product/{id}
     */
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable("id") Long id) {
        Product product = productService.getProductDetail(id);
        return Result.success(product);
    }
}
