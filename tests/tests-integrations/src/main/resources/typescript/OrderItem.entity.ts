@Entity
@Table("ORDER_ITEMS")
export class OrderItem {

    @Id
    @Generated("sequence")
    @Column({ name: "ORDERITEM_ID", type: "long" })
    public id: number;
    
    @Column({ name: "ORDER_ID", type: "long" })
    public orderId: number;
    
    @Column({ name: "ORDERITEM_NAME", type: "string" })
    public name: string;
}