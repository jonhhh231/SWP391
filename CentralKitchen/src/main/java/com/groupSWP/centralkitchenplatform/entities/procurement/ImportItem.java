package com.groupSWP.centralkitchenplatform.entities.procurement;


import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "import_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ImportItem {

    @EmbeddedId
    private ImportItemKey id;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal importPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ticketId")
    @JoinColumn(name = "ticket_id")
    private ImportTicket importTicket;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;
}