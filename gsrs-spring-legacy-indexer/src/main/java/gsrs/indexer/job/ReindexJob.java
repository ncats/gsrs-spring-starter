package gsrs.indexer.job;

import gsrs.util.TaskListener;
import ix.core.EntityMapperOptions;

@EntityMapperOptions(getSelfRel = "url")
public class ReindexJob extends TaskListener {
}
