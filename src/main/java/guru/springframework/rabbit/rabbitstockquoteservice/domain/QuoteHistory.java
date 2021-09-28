package guru.springframework.rabbit.rabbitstockquoteservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuoteHistory {

    private String  id;
    private String ticker;
    private BigDecimal price;
    private Instant instant;
}
