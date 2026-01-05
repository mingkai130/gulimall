package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.entity.product.*;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.product.BaseAttrs;
import com.atguigu.common.vo.product.Bounds;
import com.atguigu.common.vo.product.Skus;
import com.atguigu.common.vo.product.SpuSaveVO;
import com.atguigu.gulimall.product.dao.SpuInfoDao;
import com.atguigu.gulimall.product.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    SpuImagesService spuImagesService;
    @Autowired
    ProductAttrValueService productAttrValueService;
    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    BrandService brandService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    AttrService attrService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(w -> {
                w.eq("id", key).or().like("spu_name", key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId)) {
            wrapper.eq("catalog_id", catelogId);
        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId)) {
            wrapper.eq("brand_id", brandId);
        }

        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq("publish_status", status);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 发布商品
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVO vo) {
        // 1.保存spu基本信息 pms_spu_info
        SpuInfoEntity spuInfo = new SpuInfoEntity();
        // MetaObjectHandler处理时间
        BeanUtils.copyProperties(vo, spuInfo);
        Date now = new Date();
        spuInfo.setCreateTime(now);
        spuInfo.setUpdateTime(now);
        this.saveBaseSpuInfo(spuInfo);
        Long spuId = spuInfo.getId();

        // 2.保存spu描述图片（商品介绍里面的图） pms_spu_info_desc
        List<String> decript = vo.getDecript();
        spuInfoDescService.saveSpuInfoDesc(spuId, decript);

        // 3.保存spu图片集（商品展示图） pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveSpuImages(spuId, images);

        // 4.保存spu基本参数值 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        productAttrValueService.saveProductAttrValue(spuId, baseAttrs);


        List<Skus> skus = vo.getSkus();
        skuInfoService.saveSkuInfo(spuInfo, skus);

    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfo) {
        this.baseMapper.insert(spuInfo);
    }

    @Override
    public void up(Long spuId) {

    }

    @Override
    public SpuInfoEntity getBySkuId(Long skuId) {
        return null;
    }

}

