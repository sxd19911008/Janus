package com.ethan.janus.starter.dao;

import com.ethan.janus.starter.dto.TestRollbackEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 测试回滚表 Mapper
 */
@Mapper
public interface TestRollbackMapper {

    @Insert("INSERT INTO test_rollback (tbl_key, tbl_num) VALUES (#{tblKey}, #{tblNum})")
    void insert(TestRollbackEntity entity);

    @Update("UPDATE test_rollback SET tbl_num = #{tblNum} WHERE tbl_key = #{tblKey}")
    void updateByKey(@Param("tblKey") String tblKey, @Param("tblNum") Integer tblNum);

    @Delete("DELETE FROM test_rollback WHERE tbl_key = #{tblKey}")
    void deleteByKey(@Param("tblKey") String tblKey);

    @Select("SELECT tbl_num FROM test_rollback WHERE tbl_key = #{tblKey}")
    Integer selectNumByKey(@Param("tblKey") String tblKey);
    @Select("SELECT tbl_key tblKey, tbl_num tblNum FROM test_rollback WHERE tbl_key = #{tblKey}")
    List<TestRollbackEntity> selectByKey(@Param("tblKey") String tblKey);
}
