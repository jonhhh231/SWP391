package com.groupSWP.centralkitchenplatform.entities.procurement;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportItemKey implements Serializable {

    @Column(name = "ticket_id")
    private String ticketId;

    @Column(name = "ingredient_id")
    private String ingredientId;
}
