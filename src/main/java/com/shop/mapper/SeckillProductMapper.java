package com.shop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.entity.SeckillProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SeckillProductMapper extends BaseMapper<SeckillProduct> {

    /**
     * 扣减数据库库存（最终一致性）
     */
    @Update("UPDATE seckill_product SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int reduceStock(Long id);

    /**
     * 获取正在进行中的秒杀商品
     */
    @Select("SELECT * FROM seckill_product WHERE status = 1 AND start_time <= NOW() AND end_time >= NOW()")
    List<SeckillProduct> getActiveSeckillProducts();
}