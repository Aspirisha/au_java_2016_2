package cssort.profiler;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Created by andy on 2/19/17.
 */
public class DatasetHolder {
    final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    String xName;

    public DatasetHolder(String xName) {
        this.xName = xName;
    }

    void addTriplet(int varargValue, long sortTime, long requestTime, long clientTime) {
        dataset.addValue((Number) sortTime, "Sort Time", varargValue);
        dataset.addValue((Number) requestTime, "Request Time", varargValue);
        dataset.addValue((Number) clientTime, "Client Time", varargValue);
    }

    void reset(String xName) {
        dataset.clear();
        this.xName = xName;
    }
}
