package com.game.db.service.async;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.game.db.common.DbServiceName;
import com.game.db.common.Loggers;
import com.game.db.common.enums.DbOperationEnum;
import com.game.db.entity.AbstractEntity;
import com.game.db.service.common.service.IDbService;
import com.game.db.service.entity.EntityService;
import com.game.db.service.redis.AsyncRedisKeyEnum;
import com.game.db.service.redis.RedisInterface;
import com.game.db.service.redis.RedisListInterface;
import com.game.db.service.redis.RedisService;
import com.game.db.util.EntityUtils;

/**
 * db异步注册数据通知中心
 * 先把同类型的玩家数据都放在一个集合里，来保证一个玩家的操作是顺序执行的
 * 
 * 然后把玩家变动通知，放入到玩家身上
 * @author JiangBangMing
 *
 * 2018年6月21日 下午2:01:12
 */
@Service
public class AsyncDbRegisterCenter implements IDbService{
	
	private Logger logger=Loggers.dbLogger;
	@Autowired
	private RedisService redisService;
	
	/**
	 * 异步个体更新
	 * @param entityService
	 * @param dbOperationEnum
	 * @param entity
	 */
	public void asyncRegisterEntity(EntityService entityService,DbOperationEnum dbOperationEnum,AbstractEntity entity) {
		AsyncEntityWrapper asyncEntityWrapper=null;
		if(dbOperationEnum.equals(DbOperationEnum.insert)||dbOperationEnum.equals(DbOperationEnum.delete)) {
			Map<String, String> map=EntityUtils.getCacheValueMap(entity);
			asyncEntityWrapper=new AsyncEntityWrapper(dbOperationEnum,map);
		}else if(dbOperationEnum.equals(DbOperationEnum.update)) {
			Map<String, String> map=EntityUtils.getProxyChangedCacheValueMap(entity);
			asyncEntityWrapper=new AsyncEntityWrapper(dbOperationEnum,map);
		}
		asyncEntity(entityService, asyncEntityWrapper, entity);
		if(logger.isDebugEnabled()) {
			logger.debug("async register entity "+entity.getClass().getSimpleName()+" id:"+entity.getId()+" userId:"+entity.getUserId());
		}
	}
	/**
	 * 异步批量更新
	 * @param entityService
	 * @param dbOperationEnum
	 * @param entityList
	 */
	public void asyncBatchRegisterEntity(EntityService entityService,DbOperationEnum dbOperationEnum,List<AbstractEntity> entityList) {
		AsyncEntityWrapper asyncEntityWrapper=null;
		if(entityList.size()>0) {
			if(dbOperationEnum.equals(DbOperationEnum.insertBatch)||dbOperationEnum.equals(DbOperationEnum.deleteBatch)) {
				List<Map<String, String>> paramList=new ArrayList<>();
				for(AbstractEntity entity:entityList) {
					Map<String, String> map=EntityUtils.getCacheValueMap(entity);
					paramList.add(map);
					if(logger.isDebugEnabled()) {
						logger.debug("async batch register entity "+entity.getClass().getSimpleName()+" id:"+entity.getId()+" userId:"+entity.getUserId());
					}
				}
				asyncEntityWrapper=new AsyncEntityWrapper(dbOperationEnum, paramList);
			}else if(dbOperationEnum.equals(DbOperationEnum.updateBatch)) {
				List<Map<String, String>> paramList=new ArrayList<>();
				for(AbstractEntity entity:entityList) {
					Map<String, String> map=EntityUtils.getProxyChangedCacheValueMap(entity);
					paramList.add(map);
					if(logger.isDebugEnabled()) {
						logger.debug("async batch register entity "+entity.getClass().getSimpleName()+" id:"+entity.getId()+" userId:"+entity.getUserId());
					}
				}
				asyncEntityWrapper=new AsyncEntityWrapper(dbOperationEnum, paramList);
			}
			AbstractEntity entity=entityList.get(0);
			asyncEntity(entityService, asyncEntityWrapper, entity);
		}else {
			logger.debug("async batch register entity null dboperationEnum is"+dbOperationEnum);
		}
	}
	
	@Override
	public String getDbServiceName() {
		return DbServiceName.asyncDbRegisterCenter;
	}

	@Override
	public void startUp() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() throws Exception {
		// TODO Auto-generated method stub
		
	}

	private void asyncEntity(EntityService entityService,AsyncEntityWrapper asyncEntityWrapper,AbstractEntity entity) {
		//计算处于那个db
		long selectId=entityService.getShardingId(entity);
		int dbSelectId=entityService.getDefaultEntityServiceShardingStrategy().getShardingDBTableIndexByUserId(selectId);
		
		//加入到异步更新队列
		String unionKey=null;
		if(entity instanceof RedisInterface) {
			unionKey=((RedisInterface)entity).getUnionKey();
		}else if(entity instanceof RedisListInterface){
			unionKey=((RedisListInterface)entity).getShardingKey();
		} 
		
		//必须先push 再sadd
		String simapleClassName=entity.getClass().getSimpleName();
		String aysncUnionKey=simapleClassName+"#"+unionKey;
		redisService.rpushString(aysncUnionKey, asyncEntityWrapper.serialize());
		redisService.saddString(AsyncRedisKeyEnum.ASYNC_DB.getKey()+dbSelectId+"#"+entity.getClass().getSimpleName(),aysncUnionKey);
	}
}
