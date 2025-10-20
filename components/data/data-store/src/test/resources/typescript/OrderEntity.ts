@Entity()
@Table("ORDERS")
export class Order {
    
    @Id()
    @Generated("sequence")
    @Column({ name: "ORDER_ID", type: "long" })
    public id: number;

    @Column({ name: "ORDER_NUMBER", type: "string" })
    public number: string;

    @OneToMany({
		entityName: "OrderItem",
        table: "ORDER_ITEMS",
        joinColumn: "ORDER_ID",
        joinColumnNotNull: true,
        cascade: "all",
        inverse: false,
        lazy: false,
        fetch: "select"
    })
    public items: OrderItem[];
}