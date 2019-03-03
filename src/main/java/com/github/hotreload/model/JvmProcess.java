package com.github.hotreload.model;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.util.List;

import com.google.common.base.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjikai
 * Created on 2019-02-23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JvmProcess {
    private String pid;
    private String displayName;
    private String detailVmArgs;

    public boolean containsKeywords(List<String> keywords) {
        String text = toString();
        for (String k : keywords) {
            if (!text.contains(k)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        pid = firstNonNull(pid, "");
        displayName = firstNonNull(displayName, "");
        detailVmArgs = firstNonNull(detailVmArgs, "");
        String displayText = pid + " " + displayName + " " + detailVmArgs;
        return displayText.trim();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JvmProcess)) {
            return false;
        }
        JvmProcess that = (JvmProcess) o;
        return Objects.equal(pid, that.pid) &&
                Objects.equal(displayName, that.displayName) &&
                Objects.equal(detailVmArgs, that.detailVmArgs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pid, displayName, detailVmArgs);
    }
}
