package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.ObjectConstant;
import com.atguigu.common.entity.product.*;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.product.*;
import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and(qw -> {
                qw.eq("sku_id", key).or().like("sku_name", key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            queryWrapper.eq("catalog_Id", catelogId);
        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            queryWrapper.eq("brand_id", brandId);
        }

        // 价格区间
        String min = (String) params.get("min");
        if (!StringUtils.isEmpty(min)) {
            queryWrapper.ge("price", min);
        }

        String max = (String) params.get("max");
        if (!StringUtils.isEmpty(max)) {
            try {
                BigDecimal bigDecimal = new BigDecimal(max);
                if (bigDecimal.compareTo(BigDecimal.ZERO) == 1) {
                    queryWrapper.le("price", max);
                }
            } catch (Exception e) {
            }

        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    /**
     * 新增sku
     */
    @Override
    public void saveSkuInfo(SpuInfoEntity spuInfo, List<Skus> skus) {
        // 保存当前spu对应的所有sku信息
        if (!CollectionUtils.isEmpty(skus)) {
            skus.forEach(sku -> {
                // 获取当前sku默认图片
                String defaultImg = null;
                for (Images img : sku.getImages()) {
                    if (ObjectConstant.BooleanIntEnum.YES.getCode().equals(img.getDefaultImg())) {
                        defaultImg = img.getImgUrl();
                        break;
                    }
                }
                //   1)sku的基本信息：pms_sku_info
                SkuInfoEntity skuInfo = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfo);
                skuInfo.setSpuId(spuInfo.getId());
                skuInfo.setCatalogId(spuInfo.getCatalogId());
                skuInfo.setBrandId(spuInfo.getBrandId());
                skuInfo.setSkuDefaultImg(defaultImg);
                skuInfo.setSaleCount(0L);
                this.baseMapper.insert(skuInfo);
                Long skuId = skuInfo.getSkuId();

                //   2)sku的图片信息：pms_sku_images【未选中的图片不保存】
                List<SkuImagesEntity> skuImagesEntities = sku.getImages().stream().
                        filter(img -> !StringUtils.isEmpty(img.getImgUrl())).
                        map(img -> {
                            SkuImagesEntity skuImages = new SkuImagesEntity();
                            skuImages.setSkuId(skuId);
                            skuImages.setImgUrl(img.getImgUrl());
                            skuImages.setDefaultImg(img.getDefaultImg());
                            return skuImages;
                        }).collect(Collectors.toList());
                skuImagesService.saveBatch(skuImagesEntities);

                //   3)sku的销售属性值：pms_sku_sale_attr_value
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = sku.getAttr().stream().map(attr -> {
                    SkuSaleAttrValueEntity skuSaleAttrValue = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, skuSaleAttrValue);
                    skuSaleAttrValue.setSkuId(skuId);

                    return skuSaleAttrValue;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);

            });


        }
    }

    /**
     * 查询spuId对应的所有sku信息
     */
    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {
        return this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
    }

    /**
     * 查询skuId商品信息，封装VO返回
     */
    @Override
    public SkuItemVO item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVO result = new SkuItemVO();

        CompletableFuture<SkuInfoEntity> skuInfoFuture = CompletableFuture.supplyAsync(() -> {
            // 1.获取sku基本信息（pms_sku_info）【默认图片、标题、副标题、价格】
            SkuInfoEntity skuInfo = getById(skuId);
            result.setInfo(skuInfo);
            return skuInfo;
        }, executor);

        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> {
            // 2.获取sku图片信息（pms_sku_images）
            List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
            result.setImages(images);
        }, executor);


        CompletableFuture<Void> saleAttrFuture = skuInfoFuture.thenAcceptAsync((skuInfo) -> {
            // 4.获取当前sku所属spu下的所有销售属性组合（pms_sku_info、pms_sku_sale_attr_value）
            List<SkuItemSaleAttrVO> saleAttr = skuSaleAttrValueService.getSaleAttrBySpuId(skuInfo.getSpuId());
            result.setSaleAttr(saleAttr);
        }, executor);

        CompletableFuture<Void> descFuture = skuInfoFuture.thenAcceptAsync((skuInfo) -> {
            // 5.获取spu商品介绍（pms_spu_info_desc）【描述图片】
            SpuInfoDescEntity desc = spuInfoDescService.getById(skuInfo.getSpuId());
            result.setDesc(desc);
        }, executor);

        CompletableFuture<Void> groupAttrsFuture = skuInfoFuture.thenAcceptAsync((skuInfo) -> {
            // 6.获取spu规格参数信息（pms_product_attr_value、pms_attr_attrgroup_relation、pms_attr_group）
            List<SpuItemAttrGroupVO> groupAttrs = attrGroupService.getAttrGroupWithAttrsBySpuId(skuInfo.getSpuId(), skuInfo.getCatalogId());
            result.setGroupAttrs(groupAttrs);
        }, executor);


        return result;
    }

    @Override
    public List<SkuInfoEntity> getByIds(Collection<Long> skuIds) {
        return this.baseMapper.selectBatchIds(skuIds);
    }
}