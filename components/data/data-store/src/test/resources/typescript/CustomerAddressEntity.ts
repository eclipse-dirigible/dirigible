@Entity("CustomerAddress")
@Table("CUSTOMER_ADDRESS")
export class Customer {
    
    @Id()
    @Generated("sequence")
    @Column({ name: "ADDRESS_ID", type: "long" })
    public id: number;

    @Column({ name: "ADDRESS_CITY", type: "string" })
    public city: string;

	@ManyToOne({
			entityName: "Customer",
	        table: "CUSTOMER",
	        joinColumn: "CUSTOMER_ID",
	        notNull: true,
	        cascade: "all",
	        lazy: true
	    })
    public customer: Customer;
}