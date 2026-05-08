@Entity("Customer")
@Table("CUSTOMER")
export class Customer {
    
    @Id()
    @Generated("sequence")
    @Column({ name: "CUSTOMER_ID", type: "long" })
    public id: number;

    @Column({ name: "CUSTOMER_NAME", type: "string" })
    public name: string;

    @Column({ name: "CUSTOMER_ADDRESS", type: "string" })
    public address: string;
	
	@Column({ name: "CUSTOMER_INDUSTRY", type: "string" })
	public industry: string;
	
	@Column({ name: "CUSTOMER_SEGMENT", type: "double" })
	public segment: number;
}