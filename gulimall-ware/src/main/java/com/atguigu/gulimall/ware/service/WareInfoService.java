package com.atguigu.gulimall.ware.service;

import com.atguigu.common.vo.ware.FareVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.ware.entity.WareInfoEntity;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author zyl
 * @email 1248070230@qq.com
 * @date 2025-12-29 16:39:42
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareVO getFare(Long addrId);
}

