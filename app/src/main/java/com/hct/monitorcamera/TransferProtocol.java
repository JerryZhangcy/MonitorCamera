package com.hct.monitorcamera;

/**
 * 获取文件列表
 * "filelist"
 * 返回
 * 成功 "filelist|xxxx图片的名字xxxx|end" 异常 "filelist|-1|end"
 * <p>
 * 校准时间
 * "time|20180509180000|end"
 * 返回
 * 成功  "time|0|end" 异常 "time|-1|end"
 * <p>
 * 传输图片
 * "picture|xxxx图片的名字xxxx|end"
 * 返回
 * 成功 "picture|xxxxxx图片的数据xxxxxxxxx|end" 异常 "picture|-1|end"
 * <p>
 * 整个流程结束
 * "system|end"
 */

public class TransferProtocol {

    public static final String FILE_LIST = "filelist";

    public static final String TIME = "time";

    public static final String PICTURE = "picture";

    public static final String DELIMITER = "|";

    public static final String DELIMITER_DIV = "\\|";

    public static final String END = "end";

    public static final String UNKNOW = "unknow";

    public static final String ERROR = "-1";

    public static final int TRANSFER_SIZE = 1024;

    public static String getMessageHead(String message) {
        if (message.contains(DELIMITER)) {
            String head = message.substring(0, message.indexOf(DELIMITER));
            return head;
        }
        return null;
    }


}
