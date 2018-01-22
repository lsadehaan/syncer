package com.github.zzt93.syncer.consumer.input;

import com.github.zzt93.syncer.common.data.SyncData;
import com.github.zzt93.syncer.config.pipeline.common.Connection;
import com.github.zzt93.syncer.config.pipeline.input.Schema;
import com.github.zzt93.syncer.producer.input.mongo.DocTimestamp;
import java.util.Set;
import java.util.concurrent.BlockingDeque;

/**
 * @author zzt
 */
public class MongoLocalInputSource extends LocalInputSource implements MongoInputSource {

  private final DocTimestamp syncInitMeta;

  public MongoLocalInputSource(
      String clientId, Connection connection, Set<Schema> schemas,
      DocTimestamp syncInitMeta,
      BlockingDeque<SyncData> input) {
    super(clientId,connection,schemas,syncInitMeta,input);
    this.syncInitMeta = syncInitMeta;
  }

  @Override
  public DocTimestamp getSyncInitMeta() {
    return syncInitMeta;
  }

}
