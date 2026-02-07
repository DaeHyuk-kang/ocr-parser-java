package com.kang.ocrparser.model;

public class ParsedTicket {

    // 날짜 (YYYY-MM-DD or YYYY-MM-DD HH:mm:ss)
    private String weighingDate;

    // 차량 번호
    private String vehicleNumber;

    // 총중량(kg)
    private Integer grossWeightKg;

    // 차중량(kg)
    private Integer tareWeightKg;

    // 실중량(kg)
    private Integer netWeightKg;

    public String getWeighingDate() {
        return weighingDate;
    }

    public void setWeighingDate(String weighingDate) {
        this.weighingDate = weighingDate;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public Integer getGrossWeightKg() {
        return grossWeightKg;
    }

    public void setGrossWeightKg(Integer grossWeightKg) {
        this.grossWeightKg = grossWeightKg;
    }

    public Integer getTareWeightKg() {
        return tareWeightKg;
    }

    public void setTareWeightKg(Integer tareWeightKg) {
        this.tareWeightKg = tareWeightKg;
    }

    public Integer getNetWeightKg() {
        return netWeightKg;
    }

    public void setNetWeightKg(Integer netWeightKg) {
        this.netWeightKg = netWeightKg;
    }
}
