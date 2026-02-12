package com.ethan.janus.core.dto;

import com.ethan.janus.core.plugin.JanusPlugin;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PluginListDTO {

    // 高优先级插件，order 小于0
    private List<JanusPlugin> higherPluginList;
    // 低优先级插件，order 大于0
    private List<JanusPlugin> lowerPluginList;
}
