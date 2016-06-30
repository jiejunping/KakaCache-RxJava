package com.im4j.kakacache.rxjava.netcache;


import android.os.Environment;
import android.util.Log;

import com.im4j.kakacache.rxjava.core.CacheCore;
import com.im4j.kakacache.rxjava.core.CacheTarget;
import com.im4j.kakacache.rxjava.core.disk.converter.SerializableDiskConverter;
import com.im4j.kakacache.rxjava.core.disk.journal.LRUDiskJournal;
import com.im4j.kakacache.rxjava.core.disk.storage.FileDiskStorage;
import com.im4j.kakacache.rxjava.core.memory.journal.LRUMemoryJournal;
import com.im4j.kakacache.rxjava.core.memory.storage.SimpleMemoryStorage;
import com.im4j.kakacache.rxjava.manager.RxCacheManager;
import com.im4j.kakacache.rxjava.netcache.strategy.CacheStrategy;

import java.io.File;

import rx.Observable;

/**
 * RxJava的远程数据缓存处理
 * @version alafighting 2016-06
 */
public class RxRemoteCache {

    private RxRemoteCache() {
    }

    private static RxCacheManager cacheManager;
    private static int expires = 12 * 60 * 60 * 1000;

    private static RxCacheManager getCacheManager() {
        if (cacheManager == null) {
            File storageDir = new File(Environment.getExternalStorageDirectory(), "aaa_test");
            Log.e("RxRemoteCache", "storageDir="+storageDir.getAbsolutePath());

            storageDir.mkdirs();

            CacheCore.Builder coreBuilder = new CacheCore.Builder();
            coreBuilder.memory(new SimpleMemoryStorage());
            coreBuilder.memoryJournal(new LRUMemoryJournal());
            coreBuilder.memoryMax(10 * 1024 * 1024, 1000);
            coreBuilder.disk(new FileDiskStorage(storageDir));
            coreBuilder.diskJournal(new LRUDiskJournal());
            coreBuilder.diskMax(30 * 1024 * 1024, 10 * 1000);
            coreBuilder.diskConverter(new SerializableDiskConverter());
            CacheCore core = coreBuilder.create();
            cacheManager = new RxCacheManager(core);
        }
        return cacheManager;
    }

    public static <T> rx.Observable<T> load(String key) {
        return getCacheManager().load(key);
    }

    public static <T> rx.Observable<Boolean> save(String key, T value, CacheTarget target) {
        return getCacheManager().save(key, value, expires, target);
    }

    public static <T> rx.Observable<Boolean> save(String key, T value) {
        return save(key, value, CacheTarget.MemoryAndDisk);
    }



    public static <T> Observable.Transformer<T, ResultData<T>> transformer(String key, CacheStrategy strategy) {
        return new CacheTransformer<T>(key, strategy);
    }



    private static class CacheTransformer<T> implements Observable.Transformer<T, ResultData<T>> {
        private String key;
        private CacheStrategy<T> strategy;

        public CacheTransformer(String key, CacheStrategy<T> strategy) {
            this.key = key;
            this.strategy = strategy;
        }

        @Override
        public Observable<ResultData<T>> call(Observable<T> source) {
            return strategy.execute(key, source);
        }
    }

}
