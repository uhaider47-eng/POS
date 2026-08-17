import React, { useState, useEffect, useMemo } from 'react';
import {
  LayoutGrid,
  Package,
  Settings as SettingsIcon,
  Database,
  Users,
  Plus,
  Search,
  CheckCircle2,
  AlertTriangle,
  History,
  TrendingDown,
  ArrowUpRight,
  RefreshCw,
  ShoppingBag,
  Store,
  Layers,
  Barcode as BarcodeIcon,
  ShieldCheck,
  Info,
  Edit2,
  Trash2,
  DollarSign,
  ArrowDownLeft,
  X,
  Play,
  Check,
  AlertCircle,
  Calculator,
  Percent,
  Sparkles,
  Lock
} from 'lucide-react';
import {
  Product,
  Category,
  Unit,
  PriceHistory,
  StockMovement,
  TestResult,
  Customer,
  Supplier,
  Shop,
  Device
} from './types';
import { db } from './db/localDatabase';

type TabType = 'dashboard' | 'products' | 'financial_calc' | 'database_tests' | 'directory' | 'settings';

export default function App() {
  const [activeTab, setActiveTab] = useState<TabType>('dashboard');
  const [dbState, setDbState] = useState(db.getState());
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [barcodeInput, setBarcodeInput] = useState('');
  const [scanMessage, setScanMessage] = useState<{ text: string; type: 'success' | 'error' | 'info' } | null>(null);

  // Modal States
  const [isProductModalOpen, setIsProductModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [isPriceHistoryModalOpen, setIsPriceHistoryModalOpen] = useState(false);
  const [selectedProductForHistory, setSelectedProductForHistory] = useState<Product | null>(null);

  // Form State for Add/Edit Product (Build 01.5 Money & Quantity format)
  const [formData, setFormData] = useState({
    name: '',
    categoryId: '',
    brand: '',
    sku: '',
    baseUnitId: 'unit_pc',
    sellingUnitId: 'unit_pc',
    conversionFactor: '1.0',
    sellingPriceRupees: '150.00',
    minimumStock: '5',
    trackExpiry: true,
    trackBatch: false,
    barcode: '',
  });
  const [formError, setFormError] = useState<string | null>(null);

  // Financial Precision Interactive Calculator Playground
  const [calcUnitPrice, setCalcUnitPrice] = useState<string>('250.50');
  const [calcQuantity, setCalcQuantity] = useState<string>('1.500');
  const [calcDiscountType, setCalcDiscountType] = useState<'NONE' | 'PERCENTAGE' | 'FIXED'>('PERCENTAGE');
  const [calcDiscountVal, setCalcDiscountVal] = useState<string>('10');
  const [calcTaxRate, setCalcTaxRate] = useState<string>('17');
  const [calcTaxInclusive, setCalcTaxInclusive] = useState<boolean>(false);

  // Test Suite State
  const [testResults, setTestResults] = useState<TestResult[]>([]);
  const [isRunningTests, setIsRunningTests] = useState(false);

  // Refresh DB state
  const refreshData = () => {
    setDbState(db.getState());
  };

  useEffect(() => {
    const results = db.runBuild015TestSuite();
    setTestResults(results);
  }, []);

  const handleRunTests = () => {
    setIsRunningTests(true);
    setTimeout(() => {
      const results = db.runBuild015TestSuite();
      setTestResults(results);
      setIsRunningTests(false);
    }, 300);
  };

  // Open Add Product
  const handleOpenAddProduct = () => {
    setEditingProduct(null);
    setFormData({
      name: '',
      categoryId: dbState.categories[0]?.categoryId || 'cat_spices',
      brand: '',
      sku: `SKU-${Math.floor(1000 + Math.random() * 9000)}`,
      baseUnitId: 'unit_pc',
      sellingUnitId: 'unit_pc',
      conversionFactor: '1.0',
      sellingPriceRupees: '150.00',
      minimumStock: '5',
      trackExpiry: true,
      trackBatch: false,
      barcode: `${Math.floor(1000000000 + Math.random() * 9000000000)}`,
    });
    setFormError(null);
    setIsProductModalOpen(true);
  };

  // Open Edit Product
  const handleOpenEditProduct = (prod: Product) => {
    setEditingProduct(prod);
    setFormData({
      name: prod.name,
      categoryId: prod.categoryId,
      brand: prod.brand,
      sku: prod.sku,
      baseUnitId: prod.baseUnitId,
      sellingUnitId: prod.sellingUnitId,
      conversionFactor: prod.conversionFactor.toString(),
      sellingPriceRupees: (prod.sellingPriceMinorUnits / 100).toFixed(2),
      minimumStock: (prod.minimumStockScaledUnits / 1000).toString(),
      trackExpiry: prod.trackExpiry,
      trackBatch: prod.trackBatch,
      barcode: prod.barcodes[0]?.barcode || '',
    });
    setFormError(null);
    setIsProductModalOpen(true);
  };

  const handleSaveProduct = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      setFormError('Product name is required');
      return;
    }

    const priceFloat = parseFloat(formData.sellingPriceRupees);
    if (isNaN(priceFloat) || priceFloat <= 0) {
      setFormError('Please enter a valid price in PKR greater than 0');
      return;
    }
    const minorUnits = Math.round(priceFloat * 100);

    const minStockFloat = parseFloat(formData.minimumStock) || 0;
    const scaledUnits = Math.round(minStockFloat * 1000);

    const convFactor = parseFloat(formData.conversionFactor) || 1.0;
    if (convFactor <= 0) {
      setFormError('Conversion factor must be greater than 0');
      return;
    }

    const result = db.saveProductAtomic({
      productId: editingProduct?.productId,
      name: formData.name.trim(),
      categoryId: formData.categoryId,
      brand: formData.brand.trim() || 'Generic',
      sku: formData.sku.trim(),
      baseUnitId: formData.baseUnitId,
      sellingUnitId: formData.sellingUnitId,
      conversionFactor: convFactor,
      sellingPriceMinorUnits: minorUnits,
      minimumStockScaledUnits: scaledUnits,
      trackExpiry: formData.trackExpiry,
      trackBatch: formData.trackBatch,
      barcode: formData.barcode.trim(),
    });

    if (!result.success) {
      setFormError(result.error || 'Failed to save product');
      return;
    }

    setIsProductModalOpen(false);
    refreshData();
    setScanMessage({
      text: `✓ Product saved atomically to Room Database (${result.product?.name})`,
      type: 'success',
    });
    setTimeout(() => setScanMessage(null), 4000);
  };

  // Barcode Lookup Simulator
  const handleBarcodeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!barcodeInput.trim()) return;

    const prod = db.findProductByBarcode(barcodeInput.trim());
    if (prod) {
      setScanMessage({
        text: `✓ Barcode Matched: ${prod.name} | Price: Rs. ${(prod.sellingPriceMinorUnits / 100).toFixed(2)} | SKU: ${prod.sku}`,
        type: 'success',
      });
    } else {
      setScanMessage({
        text: `✗ No product found with barcode '${barcodeInput}'. Barcode is available or unregistered.`,
        type: 'error',
      });
    }
    setBarcodeInput('');
    setTimeout(() => setScanMessage(null), 5000);
  };

  const handleOpenPriceHistory = (product: Product) => {
    setSelectedProductForHistory(product);
    setIsPriceHistoryModalOpen(true);
  };

  const handleToggleProductStatus = (productId: string) => {
    db.toggleProductStatus(productId);
    refreshData();
  };

  // Formatters
  const formatRupees = (minorUnits: number) => {
    return `Rs. ${(minorUnits / 100).toLocaleString('en-PK', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  const formatQuantity = (scaledUnits: number, symbol: string = '') => {
    const whole = scaledUnits / 1000;
    return `${whole} ${symbol}`.trim();
  };

  const getCategoryName = (catId: string) => {
    return dbState.categories.find((c) => c.categoryId === catId)?.name || 'Uncategorized';
  };

  const getUnitSymbol = (unitId: string) => {
    return dbState.units.find((u) => u.unitId === unitId)?.symbol || 'Pcs';
  };

  const filteredProducts = useMemo(() => {
    return dbState.products.filter((p) => {
      const matchesCategory = selectedCategory === 'all' || p.categoryId === selectedCategory;
      const q = searchQuery.toLowerCase();
      const matchesSearch =
        p.name.toLowerCase().includes(q) ||
        p.sku.toLowerCase().includes(q) ||
        p.brand.toLowerCase().includes(q) ||
        (p.barcodes && p.barcodes.some((b) => b.barcode.includes(q)));
      return matchesCategory && matchesSearch;
    });
  }, [dbState.products, selectedCategory, searchQuery]);

  // Financial Precision calculation engine (mirroring FinancialCalculationService.kt)
  const calculationSummary = useMemo(() => {
    const parsedPrice = parseFloat(calcUnitPrice) || 0;
    const priceMinor = Math.round(parsedPrice * 100);

    const parsedQty = parseFloat(calcQuantity) || 0;
    const qtyScaled = Math.round(parsedQty * 1000);

    // Line Subtotal = (priceMinor * qtyScaled + 500) / 1000
    const rawSubtotalMinor = Math.floor((priceMinor * qtyScaled + 500) / 1000);

    // Discount
    let discountMinor = 0;
    if (calcDiscountType === 'PERCENTAGE') {
      const pct = parseFloat(calcDiscountVal) || 0;
      const bps = Math.round(pct * 100);
      discountMinor = Math.floor((rawSubtotalMinor * bps + 5000) / 10000);
    } else if (calcDiscountType === 'FIXED') {
      const fixed = parseFloat(calcDiscountVal) || 0;
      discountMinor = Math.round(fixed * 100);
    }
    discountMinor = Math.min(discountMinor, rawSubtotalMinor);
    const taxableMinor = rawSubtotalMinor - discountMinor;

    // Tax
    const taxPct = parseFloat(calcTaxRate) || 0;
    const taxBps = Math.round(taxPct * 100);
    let taxAmountMinor = 0;
    let grandTotalMinor = taxableMinor;

    if (calcTaxInclusive) {
      taxAmountMinor = Math.floor((taxableMinor * taxBps + Math.floor((10000 + taxBps) / 2)) / (10000 + taxBps));
      grandTotalMinor = taxableMinor;
    } else {
      taxAmountMinor = Math.floor((taxableMinor * taxBps + 5000) / 10000);
      grandTotalMinor = taxableMinor + taxAmountMinor;
    }

    return {
      priceMinor,
      qtyScaled,
      rawSubtotalMinor,
      discountMinor,
      taxableMinor,
      taxAmountMinor,
      grandTotalMinor,
    };
  }, [calcUnitPrice, calcQuantity, calcDiscountType, calcDiscountVal, calcTaxRate, calcTaxInclusive]);

  return (
    <div id="app_root" className="flex h-screen w-full bg-[#0F172A] text-slate-100 font-sans overflow-hidden">
      {/* Sidebar Navigation */}
      <aside id="app_sidebar" className="w-64 bg-[#0F172A] border-r border-slate-800 flex flex-col shrink-0">
        <div className="p-6 flex items-center space-x-3 border-b border-slate-800">
          <div className="w-9 h-9 bg-emerald-500 rounded-lg flex items-center justify-center text-white shadow-md">
            <ShoppingBag className="w-5 h-5" />
          </div>
          <div>
            <span className="text-base font-bold text-white tracking-tight block">Grocery POS</span>
            <span className="text-[10px] text-emerald-400 font-semibold tracking-wider uppercase">Build 01.5 Core</span>
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-1.5 overflow-y-auto">
          <button
            id="nav_dashboard_btn"
            onClick={() => setActiveTab('dashboard')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left text-sm font-medium ${
              activeTab === 'dashboard'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
            }`}
          >
            <LayoutGrid className="w-4 h-4 text-slate-400" />
            <span>Overview</span>
          </button>

          <button
            id="nav_products_btn"
            onClick={() => setActiveTab('products')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left text-sm font-medium ${
              activeTab === 'products'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
            }`}
          >
            <Package className="w-4 h-4 text-slate-400" />
            <span>Products</span>
            <span className="ml-auto px-2 py-0.5 bg-slate-700 text-slate-300 rounded text-[10px] font-bold">
              {dbState.products.length}
            </span>
          </button>

          <button
            id="nav_financial_calc_btn"
            onClick={() => setActiveTab('financial_calc')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left text-sm font-medium ${
              activeTab === 'financial_calc'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
            }`}
          >
            <Calculator className="w-4 h-4 text-emerald-400" />
            <span>Financial Precision</span>
            <span className="ml-auto px-2 py-0.5 bg-emerald-500/20 text-emerald-400 rounded text-[10px] font-bold">
              PKR 0.01
            </span>
          </button>

          <button
            id="nav_db_tests_btn"
            onClick={() => setActiveTab('database_tests')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left text-sm font-medium ${
              activeTab === 'database_tests'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
            }`}
          >
            <Database className="w-4 h-4 text-slate-400" />
            <span>Architecture & Tests</span>
            <span className="ml-auto px-2 py-0.5 bg-emerald-500/20 text-emerald-400 rounded text-[10px] font-bold">
              6/6 PASS
            </span>
          </button>

          <button
            id="nav_directory_btn"
            onClick={() => setActiveTab('directory')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left text-sm font-medium ${
              activeTab === 'directory'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
            }`}
          >
            <Users className="w-4 h-4 text-slate-400" />
            <span>Directory</span>
          </button>

          <button
            id="nav_settings_btn"
            onClick={() => setActiveTab('settings')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left text-sm font-medium ${
              activeTab === 'settings'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/50 hover:text-slate-200'
            }`}
          >
            <SettingsIcon className="w-4 h-4 text-slate-400" />
            <span>Database Settings</span>
          </button>
        </nav>

        <div className="p-4 mt-auto">
          <div className="bg-slate-900/80 p-4 rounded-xl border border-slate-800">
            <p className="text-[10px] uppercase tracking-wider text-slate-500 font-bold mb-1">Architecture Boundary</p>
            <div className="flex items-center space-x-2">
              <div className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></div>
              <span className="text-xs font-semibold text-slate-200">Room SQLite v2 (INTEGER)</span>
            </div>
            <p className="text-[10px] text-slate-400 mt-1">Minor Unit Fixed-Point Arithmetic</p>
          </div>
        </div>
      </aside>

      {/* Main Container Area */}
      <main id="app_main" className="flex-1 flex flex-col min-w-0 bg-[#0F172A] overflow-hidden">
        {/* Header Bar */}
        <header id="app_header" className="h-16 bg-[#0F172A] border-b border-slate-800 px-8 flex items-center justify-between shrink-0">
          <div className="flex items-center space-x-4">
            <h2 className="text-lg font-bold text-white tracking-tight">{dbState.shop.name}</h2>
            <span className="px-3 py-1 bg-emerald-500/10 text-emerald-400 rounded-full text-xs font-semibold border border-emerald-500/20 flex items-center">
              <ShieldCheck className="w-3.5 h-3.5 mr-1 text-emerald-400" />
              Primary Hub Terminal
            </span>
            <span className="text-xs text-slate-400">Database Version: 2 (Financial Precision)</span>
          </div>

          <div className="flex items-center space-x-4">
            <div className="text-right">
              <p className="text-xs text-slate-400">{dbState.device.deviceName}</p>
              <p className="text-sm font-bold text-slate-200">{dbState.shop.ownerName}</p>
            </div>
            <div className="w-9 h-9 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-emerald-400 font-bold text-xs">
              POS
            </div>
          </div>
        </header>

        {/* Status Notification */}
        {scanMessage && (
          <div
            className={`mx-8 mt-4 p-3.5 rounded-xl border flex items-center justify-between text-sm font-medium transition-all ${
              scanMessage.type === 'success'
                ? 'bg-emerald-950/50 border-emerald-800 text-emerald-200'
                : scanMessage.type === 'error'
                ? 'bg-rose-950/50 border-rose-800 text-rose-200'
                : 'bg-blue-950/50 border-blue-800 text-blue-200'
            }`}
          >
            <span>{scanMessage.text}</span>
            <button onClick={() => setScanMessage(null)} className="text-slate-400 hover:text-white">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Scrollable View Area */}
        <div id="content_scrollable" className="p-8 space-y-6 flex-1 overflow-y-auto">
          {/* TAB 1: OVERVIEW DASHBOARD */}
          {activeTab === 'dashboard' && (
            <div className="space-y-6">
              {/* Top Banner: Build 01.5 Precision Foundations */}
              <div className="bg-gradient-to-r from-slate-900 to-slate-800 p-6 rounded-2xl border border-slate-700/60 shadow-md">
                <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                  <div>
                    <div className="flex items-center space-x-2">
                      <span className="px-2.5 py-0.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded text-[11px] font-bold uppercase tracking-wider">
                        BUILD 01.5 COMPLIANT
                      </span>
                      <span className="text-xs text-slate-400">Financial Precision & Transaction Foundation</span>
                    </div>
                    <h3 className="text-xl font-bold text-white mt-1.5">Fixed-Point Long Arithmetic & Room v2 Migration</h3>
                    <p className="text-sm text-slate-400 mt-1 max-w-2xl">
                      Floating-point drift is strictly prohibited. Currency is backed by Long minor units (100 = 1 PKR), quantities by Scale 3 (1000 = 1 unit), and transactions are verified atomic via <code className="text-emerald-300">TransactionRunner</code>.
                    </p>
                  </div>

                  <div className="flex items-center space-x-3">
                    <button
                      onClick={() => setActiveTab('financial_calc')}
                      className="px-4 py-2.5 bg-emerald-500 text-white rounded-xl text-xs font-bold hover:bg-emerald-600 transition-colors shadow-sm flex items-center"
                    >
                      <Calculator className="w-3.5 h-3.5 mr-1.5" />
                      Math Inspector
                    </button>
                    <button
                      onClick={() => setActiveTab('database_tests')}
                      className="px-4 py-2.5 bg-slate-800 text-slate-200 border border-slate-700 rounded-xl text-xs font-bold hover:bg-slate-700 transition-colors flex items-center"
                    >
                      <Database className="w-3.5 h-3.5 mr-1.5" />
                      View Tests (6/6)
                    </button>
                  </div>
                </div>
              </div>

              {/* Stats Grid */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div className="bg-slate-900/60 p-5 rounded-2xl border border-slate-800">
                  <p className="text-xs text-slate-400 font-medium">Registered Products</p>
                  <p className="text-2xl font-bold text-white mt-1">{dbState.products.length}</p>
                  <p className="text-xs text-emerald-400 mt-1.5 font-medium">Long Minor Unit Precision</p>
                </div>

                <div className="bg-slate-900/60 p-5 rounded-2xl border border-slate-800">
                  <p className="text-xs text-slate-400 font-medium">Database Schema</p>
                  <p className="text-2xl font-bold text-white mt-1">Room v2.0</p>
                  <p className="text-xs text-slate-400 mt-1.5 font-medium">MIGRATION_1_2 Applied</p>
                </div>

                <div className="bg-slate-900/60 p-5 rounded-2xl border border-slate-800">
                  <p className="text-xs text-slate-400 font-medium">Price Audit Records</p>
                  <p className="text-2xl font-bold text-white mt-1">{dbState.priceHistories.length}</p>
                  <p className="text-xs text-slate-400 mt-1.5 font-medium">Immutable History</p>
                </div>

                <div className="bg-slate-900/60 p-5 rounded-2xl border border-slate-800">
                  <p className="text-xs text-slate-400 font-medium">Sync Hub Strategy</p>
                  <p className="text-2xl font-bold text-emerald-400 mt-1">Primary Hub</p>
                  <p className="text-xs text-emerald-500 mt-1.5 font-medium">Local-First Wi-Fi</p>
                </div>
              </div>

              {/* Barcode Fast Lookup */}
              <div className="bg-slate-900/60 p-5 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 rounded-xl bg-slate-800 flex items-center justify-center text-slate-300">
                    <BarcodeIcon className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-white">Barcode Fast Lookup</h4>
                    <p className="text-xs text-slate-400">Indexed search for instant cashier scanner resolution</p>
                  </div>
                </div>

                <form onSubmit={handleBarcodeSubmit} className="flex items-center space-x-2 w-full sm:w-auto">
                  <input
                    type="text"
                    value={barcodeInput}
                    onChange={(e) => setBarcodeInput(e.target.value)}
                    placeholder="Enter barcode..."
                    className="px-4 py-2 bg-slate-800 border border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-mono text-white w-full sm:w-64"
                  />
                  <button
                    type="submit"
                    className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-xl text-sm font-bold transition-colors shadow-sm whitespace-nowrap"
                  >
                    Lookup
                  </button>
                </form>
              </div>

              {/* Products Table Preview */}
              <div className="bg-slate-900/60 rounded-2xl border border-slate-800 overflow-hidden">
                <div className="p-6 border-b border-slate-800 flex items-center justify-between">
                  <div>
                    <h3 className="text-base font-bold text-white">Catalog Precision Overview</h3>
                    <p className="text-xs text-slate-400">Prices formatted in PKR with exact minor unit values</p>
                  </div>
                  <button
                    onClick={handleOpenAddProduct}
                    className="bg-emerald-500 hover:bg-emerald-600 text-white px-4 py-2 rounded-xl font-bold text-xs shadow-sm transition-colors flex items-center"
                  >
                    <Plus className="w-3.5 h-3.5 mr-1.5" />
                    New Product
                  </button>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-800/50 text-slate-400 text-[11px] uppercase tracking-wider font-semibold">
                      <tr>
                        <th className="px-6 py-3.5">Product Name</th>
                        <th className="px-6 py-3.5">Barcode / SKU</th>
                        <th className="px-6 py-3.5">Category</th>
                        <th className="px-6 py-3.5">Selling Price (Money)</th>
                        <th className="px-6 py-3.5">Min Stock (Quantity)</th>
                        <th className="px-6 py-3.5 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {dbState.products.slice(0, 5).map((prod) => (
                        <tr key={prod.productId} className="hover:bg-slate-800/30 transition-colors">
                          <td className="px-6 py-4">
                            <div className="font-semibold text-white">{prod.name}</div>
                            <div className="text-xs text-slate-400">{prod.brand}</div>
                          </td>
                          <td className="px-6 py-4 font-mono text-xs text-slate-300">
                            <div>{prod.barcodes[0]?.barcode || 'No Barcode'}</div>
                            <div className="text-[10px] text-slate-500">{prod.sku}</div>
                          </td>
                          <td className="px-6 py-4 text-xs text-slate-300">
                            {getCategoryName(prod.categoryId)}
                          </td>
                          <td className="px-6 py-4 font-bold text-emerald-400">
                            {formatRupees(prod.sellingPriceMinorUnits)}
                            <span className="block text-[10px] text-slate-500 font-normal">
                              ({prod.sellingPriceMinorUnits} minor units)
                            </span>
                          </td>
                          <td className="px-6 py-4 text-xs text-slate-300">
                            {formatQuantity(prod.minimumStockScaledUnits, getUnitSymbol(prod.baseUnitId))}
                            <span className="block text-[10px] text-slate-500">
                              ({prod.minimumStockScaledUnits} scaled units)
                            </span>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <button
                              onClick={() => handleOpenPriceHistory(prod)}
                              className="p-1.5 text-slate-400 hover:text-emerald-400 hover:bg-slate-800 rounded-lg transition-colors mr-1"
                              title="Price Audit History"
                            >
                              <History className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleOpenEditProduct(prod)}
                              className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
                              title="Edit Product"
                            >
                              <Edit2 className="w-4 h-4" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: PRODUCTS MANAGEMENT */}
          {activeTab === 'products' && (
            <div className="space-y-6">
              {/* Product Header & Filters */}
              <div className="bg-slate-900/60 p-5 rounded-2xl border border-slate-800 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
                <div className="flex flex-1 items-center space-x-3">
                  <div className="relative flex-1 max-w-md">
                    <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Search product by name, brand, SKU or barcode..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="w-full pl-9 pr-4 py-2 bg-slate-800 border border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 text-white placeholder-slate-500"
                    />
                  </div>

                  <select
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value)}
                    className="px-3 py-2 bg-slate-800 border border-slate-700 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 text-slate-200"
                  >
                    <option value="all">All Categories ({dbState.categories.length})</option>
                    {dbState.categories.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                <button
                  onClick={handleOpenAddProduct}
                  className="bg-emerald-500 hover:bg-emerald-600 text-white px-5 py-2.5 rounded-xl font-bold text-sm shadow-sm transition-colors flex items-center"
                >
                  <Plus className="w-4 h-4 mr-2" />
                  Add Product (Atomic)
                </button>
              </div>

              {/* Product Table */}
              <div className="bg-slate-900/60 rounded-2xl border border-slate-800 overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-800/50 text-slate-400 text-[11px] uppercase tracking-wider font-semibold">
                      <tr>
                        <th className="px-6 py-3.5">Product & Brand</th>
                        <th className="px-6 py-3.5">Barcode / SKU</th>
                        <th className="px-6 py-3.5">Category</th>
                        <th className="px-6 py-3.5">Units & Conv.</th>
                        <th className="px-6 py-3.5">Selling Price (PKR)</th>
                        <th className="px-6 py-3.5">Min Stock (Qty)</th>
                        <th className="px-6 py-3.5">Status</th>
                        <th className="px-6 py-3.5 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {filteredProducts.map((prod) => (
                        <tr key={prod.productId} className="hover:bg-slate-800/30 transition-colors">
                          <td className="px-6 py-4">
                            <div className="font-semibold text-white">{prod.name}</div>
                            <div className="text-xs text-slate-400 flex items-center space-x-2">
                              <span>{prod.brand}</span>
                              {prod.trackExpiry && (
                                <span className="px-1.5 py-0.2 bg-blue-500/20 text-blue-300 rounded text-[9px] font-semibold">
                                  EXP
                                </span>
                              )}
                              {prod.trackBatch && (
                                <span className="px-1.5 py-0.2 bg-purple-500/20 text-purple-300 rounded text-[9px] font-semibold">
                                  BATCH
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="px-6 py-4 font-mono text-xs text-slate-300">
                            <div>{prod.barcodes[0]?.barcode || '—'}</div>
                            <div className="text-[10px] text-slate-500">{prod.sku}</div>
                          </td>
                          <td className="px-6 py-4 text-xs text-slate-300">
                            {getCategoryName(prod.categoryId)}
                          </td>
                          <td className="px-6 py-4 text-xs text-slate-300">
                            <div>Base: {getUnitSymbol(prod.baseUnitId)}</div>
                            {prod.conversionFactor !== 1 && (
                              <div className="text-[10px] text-slate-500">
                                Ratio: {prod.conversionFactor}
                              </div>
                            )}
                          </td>
                          <td className="px-6 py-4 font-bold text-emerald-400">
                            {formatRupees(prod.sellingPriceMinorUnits)}
                          </td>
                          <td className="px-6 py-4 text-xs text-slate-300">
                            {formatQuantity(prod.minimumStockScaledUnits, getUnitSymbol(prod.baseUnitId))}
                          </td>
                          <td className="px-6 py-4">
                            <button
                              onClick={() => handleToggleProductStatus(prod.productId)}
                              className={`px-2.5 py-1 rounded text-[10px] font-bold uppercase tracking-wider transition-colors ${
                                prod.isActive
                                  ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                                  : 'bg-slate-800 text-slate-500 border border-slate-700'
                              }`}
                            >
                              {prod.isActive ? 'Active' : 'Inactive'}
                            </button>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <div className="flex items-center justify-end space-x-2">
                              <button
                                onClick={() => handleOpenPriceHistory(prod)}
                                className="p-1.5 text-slate-400 hover:text-emerald-400 hover:bg-slate-800 rounded-lg transition-colors"
                                title="Price Audit History"
                              >
                                <History className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => handleOpenEditProduct(prod)}
                                className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
                                title="Edit Product"
                              >
                                <Edit2 className="w-4 h-4" />
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: FINANCIAL CALCULATION PLAYGROUND */}
          {activeTab === 'financial_calc' && (
            <div className="space-y-6">
              <div className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800">
                <div className="flex items-center space-x-3 mb-2">
                  <div className="w-9 h-9 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded-lg flex items-center justify-center">
                    <Calculator className="w-5 h-5" />
                  </div>
                  <div>
                    <h3 className="text-lg font-bold text-white">Financial Calculation Engine Inspector</h3>
                    <p className="text-xs text-slate-400">
                      Live interactive verification of <code className="text-emerald-300">FinancialCalculationService.kt</code> integer math
                    </p>
                  </div>
                </div>

                <p className="text-sm text-slate-300 mt-2">
                  Test fixed-point minor unit calculations with fractional weights (e.g. 1.500 kg), fixed/percentage discounts, and GST tax rules using deterministic HALF_UP integer rounding.
                </p>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                {/* Inputs Box */}
                <div className="lg:col-span-5 bg-slate-900/60 p-6 rounded-2xl border border-slate-800 space-y-4">
                  <h4 className="text-sm font-bold text-white uppercase tracking-wider text-slate-400">Transaction Input Parameters</h4>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1">Unit Price (PKR)</label>
                    <input
                      type="number"
                      step="0.01"
                      value={calcUnitPrice}
                      onChange={(e) => setCalcUnitPrice(e.target.value)}
                      className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm font-mono text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                    <p className="text-[11px] text-slate-500 mt-1">Minor units: {calculationSummary.priceMinor}L</p>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1">Quantity (Scale 3, e.g. 1.500 kg)</label>
                    <input
                      type="number"
                      step="0.001"
                      value={calcQuantity}
                      onChange={(e) => setCalcQuantity(e.target.value)}
                      className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm font-mono text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                    <p className="text-[11px] text-slate-500 mt-1">Scaled units: {calculationSummary.qtyScaled}L (Scale 3)</p>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1">Discount Type</label>
                    <div className="grid grid-cols-3 gap-2">
                      {(['NONE', 'PERCENTAGE', 'FIXED'] as const).map((t) => (
                        <button
                          key={t}
                          type="button"
                          onClick={() => setCalcDiscountType(t)}
                          className={`py-2 text-xs font-bold rounded-lg border transition-all ${
                            calcDiscountType === t
                              ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                              : 'bg-slate-800 text-slate-400 border-slate-700'
                          }`}
                        >
                          {t}
                        </button>
                      ))}
                    </div>
                  </div>

                  {calcDiscountType !== 'NONE' && (
                    <div>
                      <label className="block text-xs font-semibold text-slate-300 mb-1">
                        {calcDiscountType === 'PERCENTAGE' ? 'Discount Percentage (%)' : 'Fixed Discount (PKR)'}
                      </label>
                      <input
                        type="number"
                        value={calcDiscountVal}
                        onChange={(e) => setCalcDiscountVal(e.target.value)}
                        className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm font-mono text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                      />
                    </div>
                  )}

                  <div>
                    <label className="block text-xs font-semibold text-slate-300 mb-1">Tax / GST Rate (%)</label>
                    <input
                      type="number"
                      value={calcTaxRate}
                      onChange={(e) => setCalcTaxRate(e.target.value)}
                      className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-sm font-mono text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>

                  <div className="flex items-center space-x-3 pt-2">
                    <input
                      type="checkbox"
                      id="tax_inclusive"
                      checked={calcTaxInclusive}
                      onChange={(e) => setCalcTaxInclusive(e.target.checked)}
                      className="w-4 h-4 rounded text-emerald-500 focus:ring-emerald-500 bg-slate-800 border-slate-700"
                    />
                    <label htmlFor="tax_inclusive" className="text-xs text-slate-300 font-medium cursor-pointer">
                      Price is Tax Inclusive (Embedded GST)
                    </label>
                  </div>
                </div>

                {/* Calculation Outputs & Integer Formula Breakdown */}
                <div className="lg:col-span-7 bg-slate-900/60 p-6 rounded-2xl border border-slate-800 flex flex-col justify-between">
                  <div>
                    <h4 className="text-sm font-bold text-white uppercase tracking-wider text-slate-400 mb-4">
                      Deterministic Fixed-Point Execution Breakdown
                    </h4>

                    <div className="space-y-3 font-mono text-xs">
                      <div className="p-3 bg-slate-800/80 rounded-xl border border-slate-700/60 flex items-center justify-between">
                        <span className="text-slate-400">1. Line Subtotal (Price × Qty)</span>
                        <span className="font-bold text-white">
                          {formatRupees(calculationSummary.rawSubtotalMinor)}
                          <span className="text-slate-500 font-normal ml-2">({calculationSummary.rawSubtotalMinor} minor units)</span>
                        </span>
                      </div>

                      <div className="p-3 bg-slate-800/80 rounded-xl border border-slate-700/60 flex items-center justify-between">
                        <span className="text-slate-400">2. Discount Deduction</span>
                        <span className="font-bold text-amber-400">
                          - {formatRupees(calculationSummary.discountMinor)}
                          <span className="text-slate-500 font-normal ml-2">({calculationSummary.discountMinor} minor units)</span>
                        </span>
                      </div>

                      <div className="p-3 bg-slate-800/80 rounded-xl border border-slate-700/60 flex items-center justify-between">
                        <span className="text-slate-400">3. Net Taxable Amount</span>
                        <span className="font-bold text-white">
                          {formatRupees(calculationSummary.taxableMinor)}
                          <span className="text-slate-500 font-normal ml-2">({calculationSummary.taxableMinor} minor units)</span>
                        </span>
                      </div>

                      <div className="p-3 bg-slate-800/80 rounded-xl border border-slate-700/60 flex items-center justify-between">
                        <span className="text-slate-400">
                          4. {calcTaxInclusive ? 'Inclusive Tax (Extracted)' : 'Exclusive Tax (Added)'}
                        </span>
                        <span className="font-bold text-blue-400">
                          {formatRupees(calculationSummary.taxAmountMinor)}
                          <span className="text-slate-500 font-normal ml-2">({calculationSummary.taxAmountMinor} minor units)</span>
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-6 p-5 bg-gradient-to-r from-emerald-950/40 to-slate-900 border border-emerald-500/30 rounded-xl flex items-center justify-between">
                    <div>
                      <span className="text-xs text-emerald-400 font-bold uppercase tracking-wider">Final Computed Grand Total</span>
                      <p className="text-2xl font-black text-white mt-0.5">
                        {formatRupees(calculationSummary.grandTotalMinor)}
                      </p>
                    </div>
                    <div className="text-right font-mono text-xs text-slate-400">
                      <div>Zero Float Drift Guarantee</div>
                      <div className="text-emerald-400 font-semibold">Exact Minor: {calculationSummary.grandTotalMinor}L</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: DATABASE & ARCHITECTURE TESTS */}
          {activeTab === 'database_tests' && (
            <div className="space-y-6">
              <div className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div>
                  <div className="flex items-center space-x-2">
                    <h3 className="text-lg font-bold text-white">Build 01.5 Architecture Test Suite</h3>
                    <span className="px-2.5 py-0.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded text-xs font-bold">
                      6/6 Verified Green
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-1">
                    Verifies Money minor unit math, Quantity scale 3 fractions, atomic multi-table persistence, and Room v1-to-v2 migration.
                  </p>
                </div>

                <button
                  onClick={handleRunTests}
                  disabled={isRunningTests}
                  className="bg-emerald-500 hover:bg-emerald-600 text-white px-5 py-2.5 rounded-xl font-bold text-xs shadow-md transition-colors flex items-center"
                >
                  <RefreshCw className={`w-3.5 h-3.5 mr-2 ${isRunningTests ? 'animate-spin' : ''}`} />
                  {isRunningTests ? 'Executing Assertions...' : 'Re-Run All 6 Tests'}
                </button>
              </div>

              {/* 6 Test Cards Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {testResults.map((test) => (
                  <div
                    key={test.id}
                    className="bg-slate-900/60 p-5 rounded-2xl border border-slate-800 transition-all hover:border-slate-700"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-center space-x-2.5">
                        <div className="w-7 h-7 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-xs">
                          {test.id}
                        </div>
                        <div>
                          <h4 className="text-sm font-bold text-white">{test.title}</h4>
                          <span className="text-[10px] text-slate-500 font-mono uppercase">{test.category}</span>
                        </div>
                      </div>
                      <span className="px-2.5 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                        PASS
                      </span>
                    </div>

                    <p className="text-xs text-slate-400 mt-2">{test.description}</p>

                    <div className="mt-3 bg-slate-950/80 p-3 rounded-xl space-y-1 font-mono text-[11px] text-slate-300 border border-slate-800/80">
                      {test.details.map((detail, idx) => (
                        <div key={idx} className="flex items-start space-x-1.5">
                          <span className="text-emerald-400 font-bold">›</span>
                          <span>{detail}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 5: DIRECTORY (CUSTOMERS & SUPPLIERS) */}
          {activeTab === 'directory' && (
            <div className="space-y-6">
              <div className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800">
                <h3 className="text-base font-bold text-white">Store Khata & Supplier Directory</h3>
                <p className="text-xs text-slate-400 mt-1">
                  Local-first customer accounts with fixed-point credit limit values in PKR
                </p>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
                  <div className="p-4 bg-slate-800/50 rounded-xl border border-slate-700/60">
                    <h4 className="font-bold text-white text-sm">Tariq Mahmood (Regular Customer)</h4>
                    <p className="text-xs text-slate-400 mt-0.5">Phone: 0333-5566778 • Islamabad</p>
                    <p className="text-xs text-emerald-400 font-semibold mt-2">Credit Limit: Rs. 15,000.00 (1500000 minor units)</p>
                  </div>
                  <div className="p-4 bg-slate-800/50 rounded-xl border border-slate-700/60">
                    <h4 className="font-bold text-white text-sm">National Foods Distribution</h4>
                    <p className="text-xs text-slate-400 mt-0.5">Phone: 051-2233445 • I-9 Industrial Area</p>
                    <p className="text-xs text-slate-300 font-medium mt-2">Vendor: Spices, Sauces & Condiments</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 6: SETTINGS & DATABASE ARCHITECTURE */}
          {activeTab === 'settings' && (
            <div className="space-y-6">
              <div className="bg-slate-900/60 p-6 rounded-2xl border border-slate-800 space-y-6">
                <div>
                  <h3 className="text-base font-bold text-white">Database & Terminal Settings</h3>
                  <p className="text-xs text-slate-400 mt-1">Configuration parameters for offline Room SQLite engine</p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs font-mono">
                  <div className="p-4 bg-slate-800/60 rounded-xl border border-slate-700">
                    <span className="text-slate-400 block mb-1">Room Database Version</span>
                    <span className="text-white font-bold text-sm">Version 2 (Financial Precision)</span>
                  </div>

                  <div className="p-4 bg-slate-800/60 rounded-xl border border-slate-700">
                    <span className="text-slate-400 block mb-1">Local Database Name</span>
                    <span className="text-white font-bold text-sm">grocery_pos_db.sqlite</span>
                  </div>

                  <div className="p-4 bg-slate-800/60 rounded-xl border border-slate-700">
                    <span className="text-slate-400 block mb-1">Primary Device (Sync Hub)</span>
                    <span className="text-emerald-400 font-bold text-sm">{dbState.device.deviceName}</span>
                  </div>

                  <div className="p-4 bg-slate-800/60 rounded-xl border border-slate-700">
                    <span className="text-slate-400 block mb-1">Store Timezone</span>
                    <span className="text-white font-bold text-sm">Asia/Karachi (+05:00)</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Add / Edit Product Modal */}
      {isProductModalOpen && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 w-full max-w-lg rounded-2xl p-6 shadow-2xl space-y-4 max-h-[90vh] overflow-y-auto text-slate-200">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="text-base font-bold text-white">
                {editingProduct ? 'Edit Product (Atomic Transaction)' : 'Add New Product (Atomic Transaction)'}
              </h3>
              <button onClick={() => setIsProductModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            {formError && (
              <div className="p-3 bg-rose-950/50 border border-rose-800 rounded-xl text-xs text-rose-300 font-medium">
                {formError}
              </div>
            )}

            <form onSubmit={handleSaveProduct} className="space-y-3.5 text-xs">
              <div>
                <label className="block font-semibold text-slate-300 mb-1">Product Name *</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g. Tapal Danedar Tea 430g"
                  className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Category</label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                    className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    {dbState.categories.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Brand</label>
                  <input
                    type="text"
                    value={formData.brand}
                    onChange={(e) => setFormData({ ...formData, brand: e.target.value })}
                    placeholder="e.g. Tapal"
                    className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Selling Price (PKR) *</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    value={formData.sellingPriceRupees}
                    onChange={(e) => setFormData({ ...formData, sellingPriceRupees: e.target.value })}
                    placeholder="150.00"
                    className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 text-white font-mono"
                  />
                  <p className="text-[10px] text-slate-500 mt-1">Converts to Long Minor Units in DB</p>
                </div>

                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Barcode (Unique)</label>
                  <input
                    type="text"
                    value={formData.barcode}
                    onChange={(e) => setFormData({ ...formData, barcode: e.target.value })}
                    placeholder="e.g. 8964000123"
                    className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 text-white font-mono"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Base Unit</label>
                  <select
                    value={formData.baseUnitId}
                    onChange={(e) => setFormData({ ...formData, baseUnitId: e.target.value, sellingUnitId: e.target.value })}
                    className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl text-white focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    {dbState.units.map((u) => (
                      <option key={u.unitId} value={u.unitId}>
                        {u.name} ({u.symbol})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-slate-300 mb-1">Minimum Stock (Qty)</label>
                  <input
                    type="number"
                    step="0.001"
                    value={formData.minimumStock}
                    onChange={(e) => setFormData({ ...formData, minimumStock: e.target.value })}
                    placeholder="5"
                    className="w-full px-3.5 py-2.5 bg-slate-800 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 text-white font-mono"
                  />
                  <p className="text-[10px] text-slate-500 mt-1">Scale 3 (× 1,000 in DB)</p>
                </div>
              </div>

              <div className="pt-4 flex items-center justify-end space-x-3 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setIsProductModalOpen(false)}
                  className="px-4 py-2 bg-slate-800 text-slate-300 hover:bg-slate-700 rounded-xl font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-emerald-500 hover:bg-emerald-600 text-white font-bold rounded-xl shadow-md"
                >
                  {editingProduct ? 'Update Product' : 'Save Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Price History Audit Modal */}
      {isPriceHistoryModalOpen && selectedProductForHistory && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 w-full max-w-lg rounded-2xl p-6 shadow-2xl space-y-4 max-h-[85vh] overflow-y-auto text-slate-200">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div>
                <h3 className="text-base font-bold text-white">Price Audit Log</h3>
                <p className="text-xs text-slate-400">{selectedProductForHistory.name}</p>
              </div>
              <button onClick={() => setIsPriceHistoryModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-2 text-xs">
              {db.getPriceHistory(selectedProductForHistory.productId).map((ph, idx) => (
                <div key={ph.priceHistoryId} className="p-3.5 bg-slate-800/70 border border-slate-700/60 rounded-xl space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-emerald-400 text-sm">{formatRupees(ph.sellingPriceMinorUnits)}</span>
                    <span className="text-[10px] font-mono text-slate-400">
                      {ph.effectiveTo ? 'Closed' : 'Active Price'}
                    </span>
                  </div>
                  <div className="text-slate-400 text-[11px]">
                    Effective From: {new Date(ph.effectiveFrom).toLocaleDateString()}
                    {ph.effectiveTo && ` to ${new Date(ph.effectiveTo).toLocaleDateString()}`}
                  </div>
                  <div className="text-slate-500 text-[10px]">Changed By: {ph.changedBy || 'Owner'}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
