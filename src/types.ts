export type DeviceType = 'PHONE' | 'TABLET';
export type DeviceStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';
export type RoleName = 'OWNER' | 'MANAGER' | 'CASHIER' | 'STOCK_MANAGER';
export type UnitCode =
  | 'PIECE'
  | 'GRAM'
  | 'KILOGRAM'
  | 'MILLILITRE'
  | 'LITRE'
  | 'PACK'
  | 'BOX'
  | 'CARTON'
  | 'DOZEN'
  | 'BAG'
  | 'CUSTOM';

export type MovementType =
  | 'PURCHASE'
  | 'SALE'
  | 'SALE_RETURN'
  | 'PURCHASE_RETURN'
  | 'DAMAGE'
  | 'LOSS'
  | 'ADJUSTMENT_IN'
  | 'ADJUSTMENT_OUT'
  | 'OPENING_STOCK'
  | 'TRANSFER_IN'
  | 'TRANSFER_OUT';

export interface Shop {
  shopId: string;
  name: string;
  ownerName: string;
  phone: string;
  address: string;
  currency: string;
  timezone: string;
  createdAt: number;
  updatedAt: number;
}

export interface Device {
  deviceId: string;
  shopId: string;
  deviceName: string;
  deviceType: DeviceType;
  isPrimary: boolean;
  status: DeviceStatus;
  createdAt: number;
  lastSeenAt: number;
}

export interface Role {
  roleId: string;
  name: RoleName;
  description: string;
  permissionsJson: string;
}

export interface User {
  userId: string;
  shopId: string;
  roleId: string;
  name: string;
  phone: string;
  pinHash: string;
  isActive: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Category {
  categoryId: string;
  shopId: string;
  name: string;
  parentCategoryId?: string | null;
  isActive: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Unit {
  unitId: string;
  code: UnitCode;
  name: string;
  symbol: string;
  isCustom: boolean;
}

export interface Barcode {
  barcodeId: string;
  productId: string;
  barcode: string;
  isPrimary: boolean;
  shopId: string;
}

export interface Product {
  productId: string;
  shopId: string;
  name: string;
  categoryId: string;
  brand: string;
  sku: string;
  baseUnitId: string;
  sellingUnitId: string;
  conversionFactor: number;
  sellingPrice: number;
  minimumStock: number;
  trackExpiry: boolean;
  trackBatch: boolean;
  isActive: boolean;
  barcodes: Barcode[];
  createdAt: number;
  updatedAt: number;
}

export interface PriceHistory {
  priceHistoryId: string;
  productId: string;
  sellingPrice: number;
  effectiveFrom: number;
  effectiveTo?: number | null;
  changedBy?: string | null;
  createdAt: number;
}

export interface StockBalance {
  productId: string;
  quantity: number;
  averageCost: number;
  updatedAt: number;
}

export interface StockMovement {
  movementId: string;
  shopId: string;
  deviceId: string;
  productId: string;
  batchId?: string | null;
  movementType: MovementType;
  quantity: number;
  unitCost: number;
  referenceType?: string | null;
  referenceId?: string | null;
  createdBy: string;
  createdAt: number;
}

export interface Customer {
  customerId: string;
  shopId: string;
  name: string;
  phone: string;
  address: string;
  creditLimit: number;
  notes: string;
  isActive: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Supplier {
  supplierId: string;
  shopId: string;
  name: string;
  phone: string;
  address: string;
  notes: string;
  isActive: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface TestResult {
  id: number;
  title: string;
  description: string;
  passed: boolean;
  details: string[];
}
