@Entity("Customer")
@Table("CUSTOMER")
export class Customer {
    
    @Id
    @Generated("sequence")
    @Column({ name: "ID", type: "long" })
    public id: number;

    @Column({ name: "NAME", type: "string" })
    public name: string;

    @Column({ name: "ADDRESS", type: "string" })
    public address: string;
}