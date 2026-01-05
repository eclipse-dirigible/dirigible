@Entity('CarEntity')
@Table({ name: 'CARS' })
export class Car {

    @Id()
    @Generated('sequence')
    @Column({ name: 'car_id', type: 'int' })
    id: number;

    @Column({ type: 'varchar', length: 255 })
    make: string;

    @Column({ type: 'varchar', length: 255 })
    model: string;

    @Column({ name: 'manufacture_year', type: 'int' })
    year: number;

    @Column({ type: 'decimal', nullable: true })
    price: number | null;

    @Column({ name: 'is_electric', type: 'boolean', defaultValue: false })
    isElectric: boolean = false;

    @Column({ type: 'jsonb', nullable: true })
    features: string[] | null;
}