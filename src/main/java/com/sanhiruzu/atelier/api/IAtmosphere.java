package com.sanhiruzu.atelier.api;

/**
 * Interface for atmosphere properties in a zone.
 * Used by Create Kaizen to track air quality metrics.
 */
public interface IAtmosphere {

    /**
     * Get the chemical purity of the atmosphere (0-100+).
     * Higher values indicate cleaner air.
     */
    float getChemicalPurity();

    /**
     * Get the particulate density of the atmosphere (0-100+).
     * Higher values indicate more particles/dust.
     */
    float getParticulateDensity();

    /**
     * Get the temperature in degrees Celsius.
     */
    float getTemperature();

    /**
     * Set custom atmosphere property.
     */
    void setProperty(String key, float value);

    /**
     * Get custom atmosphere property.
     */
    float getProperty(String key, float defaultValue);
}
