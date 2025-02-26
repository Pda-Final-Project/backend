package finpago.fillingservice.entity;

import finpago.common.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "fillings")
public class Filling extends BaseEntity {
    @Id
    @Column(name = "filling_id")
    private String fillingId;

    @Column(name = "filling_title")
    private String fillingTitle;

    @Column(name = "filling_type")
    private String fillingType;

    @Column(name = "filling_ticker")
    private String fillingTicker;

    @Column(name = "filling_url")
    private String fillingUrl;

    @Column(name = "filling_file_type")
    private String fillingFileType;

    @Column(name = "filling_translated_content_url")
    private String fillingTranslatedContentUrl;

    @Column(name = "filling_10q_json_url")
    private String filling10qJsonUrl;

    @Column(name = "submit_timestamp")
    private String submitTimestamp;

}