package com.flashsale.mapper;

import com.flashsale.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper {

    /**
     * 根据ID查询商品
     */
    Product selectById(@Param("id") Long id);

    /**
     * 查询已上架商品列表
     */
    List<Product> selectOnShelfList();

    /**
     * 根据分类查询商品列表
     */
    List<Product> selectByCategory(@Param("category") String category);
}
