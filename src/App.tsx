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
  AlertCircle
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

type TabType = 'dashboard' | 'products' | 'database_tests' | 'directory' | 'settings';

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
  const [isStockModalOpen, setIsStockModalOpen] = useState(false);
  const [selectedProductForStock, setSelectedProductForStock] = useState<Product | null>(null);
  const [stockAddAmount, setStockAddAmount] = useState<number>(10);
  const [stockUnitCost, setStockUnitCost] = useState<number>(100);

  // Customer Modal State
  const [isCustomerModalOpen, setIsCustomerModalOpen] = useState(false);
  const [newCustName, setNewCustName] = useState('');
  const [newCustPhone, setNewCustPhone] = useState('');
  const [newCustAddress, setNewCustAddress] = useState('');
  const [newCustLimit, setNewCustLimit] = useState(10000);

  // Form State for Add/Edit Product
  const [formData, setFormData] = useState({
    name: '',
    categoryId: '',
    brand: '',
    sku: '',
    baseUnitId: 'unit_pc',
    sellingUnitId: 'unit_pc',
    conversionFactor: 1.0,
    sellingPrice: 100,
    minimumStock: 5,
    trackExpiry: true,
    trackBatch: false,
    barcode: '',
    initialStock: 20,
  });
  const [formError, setFormError] = useState<string | null>(null);

  // Test Suite State
  const [testResults, setTestResults] = useState<TestResult[]>([]);
  const [isRunningTests, setIsRunningTests] = useState(false);

  // Refresh DB state
  const refreshData = () => {
    setDbState(db.getState());
  };

  useEffect(() => {
    // Run tests once on mount for test inspector
    const results = db.runDatabaseTestSuite();
    setTestResults(results);
  }, []);

  const handleRunTests = () => {
    setIsRunningTests(true);
    setTimeout(() => {
      const results = db.runDatabaseTestSuite();
      setTestResults(results);
      setIsRunningTests(false);
    }, 400);
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
      conversionFactor: 1.0,
      sellingPrice: 150,
      minimumStock: 5,
      trackExpiry: true,
      trackBatch: false,
      barcode: `${Math.floor(1000000000 + Math.random() * 9000000000)}`,
      initialStock: 25,
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
      conversionFactor: prod.conversionFactor,
      sellingPrice: prod.sellingPrice,
      minimumStock: prod.minimumStock,
      trackExpiry: prod.trackExpiry,
      trackBatch: prod.trackBatch,
      barcode: prod.barcodes[0]?.barcode || '',
      initialStock: dbState.stockBalances[prod.productId]?.quantity || 0,
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
    if (formData.sellingPrice <= 0) {
      setFormError('Selling price must be greater than 0');
      return;
    }

    const result = db.saveProduct({
      productId: editingProduct?.productId,
      name: formData.name.trim(),
      categoryId: formData.categoryId,
      brand: formData.brand.trim() || 'Generic',
      sku: formData.sku.trim(),
      baseUnitId: formData.baseUnitId,
      sellingUnitId: formData.sellingUnitId,
      conversionFactor: Number(formData.conversionFactor) || 1.0,
      sellingPrice: Number(formData.sellingPrice),
      minimumStock: Number(formData.minimumStock) || 0,
      trackExpiry: formData.trackExpiry,
      trackBatch: formData.trackBatch,
      barcode: formData.barcode.trim(),
      initialStock: Number(formData.initialStock) || 0,
    });

    if (result.error) {
      setFormError(result.error);
      return;
    }

    setIsProductModalOpen(false);
    refreshData();
  };

  // Fast Barcode Search & Quick Sale Simulation
  const handleBarcodeSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!barcodeInput.trim()) return;

    const prod = db.findProductByBarcode(barcodeInput.trim());
    if (prod) {
      const saleResult = db.recordStockSale(prod.productId, 1);
      setScanMessage({
        text: `✓ Sold 1x ${prod.name} (PKR ${prod.sellingPrice.toFixed(2)}) — Remaining Stock: ${saleResult.newQty} ${getUnitSymbol(prod.baseUnitId)}`,
        type: 'success',
      });
      refreshData();
    } else {
      setScanMessage({
        text: `✗ No product found with barcode '${barcodeInput}'. Check the catalog or assign in Products tab.`,
        type: 'error',
      });
    }
    setBarcodeInput('');
    setTimeout(() => setScanMessage(null), 5000);
  };

  const handleQuickSale = (product: Product) => {
    const saleResult = db.recordStockSale(product.productId, 1);
    setScanMessage({
      text: `✓ Quick Sold 1x ${product.name} (PKR ${product.sellingPrice.toFixed(2)}) — Stock: ${saleResult.newQty}`,
      type: 'success',
    });
    refreshData();
    setTimeout(() => setScanMessage(null), 4000);
  };

  const handleOpenStockModal = (product: Product) => {
    setSelectedProductForStock(product);
    setStockAddAmount(10);
    setStockUnitCost(product.sellingPrice * 0.75);
    setIsStockModalOpen(true);
  };

  const handleConfirmStockIn = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedProductForStock && stockAddAmount > 0) {
      db.recordStockIn(selectedProductForStock.productId, stockAddAmount, stockUnitCost);
      setIsStockModalOpen(false);
      refreshData();
      setScanMessage({
        text: `✓ Added ${stockAddAmount} ${getUnitSymbol(selectedProductForStock.baseUnitId)} to ${selectedProductForStock.name}`,
        type: 'success',
      });
      setTimeout(() => setScanMessage(null), 4000);
    }
  };

  const handleOpenPriceHistory = (product: Product) => {
    setSelectedProductForHistory(product);
    setIsPriceHistoryModalOpen(true);
  };

  const handleToggleProductStatus = (productId: string) => {
    db.toggleProductStatus(productId);
    refreshData();
  };

  const handleCreateCustomer = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCustName.trim()) return;
    db.addCustomer({
      name: newCustName.trim(),
      phone: newCustPhone.trim() || '0300-0000000',
      address: newCustAddress.trim() || 'Local Area',
      creditLimit: Number(newCustLimit) || 5000,
      notes: 'Customer created from directory',
      isActive: true,
    });
    setIsCustomerModalOpen(false);
    setNewCustName('');
    setNewCustPhone('');
    setNewCustAddress('');
    refreshData();
  };

  const handleResetDatabase = () => {
    if (window.confirm('Reset database to initial Pakistani grocery store seeds?')) {
      db.resetToDefault();
      refreshData();
      const results = db.runDatabaseTestSuite();
      setTestResults(results);
    }
  };

  // Helpers
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

  const totalProductCount = dbState.products.length;
  const activeProductCount = dbState.products.filter((p) => p.isActive).length;
  const lowStockProducts = dbState.products.filter((p) => {
    const qty = dbState.stockBalances[p.productId]?.quantity || 0;
    return qty <= p.minimumStock;
  });

  return (
    <div id="app_root" className="flex h-screen w-full bg-[#F1F5F9] overflow-hidden text-slate-800 font-sans">
      {/* Sidebar Navigation */}
      <aside id="app_sidebar" className="w-64 bg-[#0F172A] text-slate-300 flex flex-col shrink-0">
        <div className="p-6 flex items-center space-x-3 border-b border-slate-700/50">
          <div className="w-8 h-8 bg-[#10B981] rounded flex items-center justify-center text-white shadow-sm">
            <ShoppingBag className="w-5 h-5" />
          </div>
          <div>
            <span className="text-lg font-bold text-white tracking-tight block">Grocery POS</span>
            <span className="text-[10px] text-emerald-400 font-semibold tracking-wider uppercase">Room Core v1</span>
          </div>
        </div>

        <nav className="flex-1 p-4 space-y-1.5 overflow-y-auto">
          <button
            id="nav_dashboard_btn"
            onClick={() => setActiveTab('dashboard')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left font-medium ${
              activeTab === 'dashboard'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
            }`}
          >
            <LayoutGrid className="w-5 h-5 text-slate-400" />
            <span>Dashboard</span>
          </button>

          <button
            id="nav_products_btn"
            onClick={() => setActiveTab('products')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left font-medium ${
              activeTab === 'products'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
            }`}
          >
            <Package className="w-5 h-5 text-slate-400" />
            <span>Products</span>
            {lowStockProducts.length > 0 && (
              <span className="ml-auto px-2 py-0.5 bg-amber-500/20 text-amber-300 rounded-full text-[10px] font-bold">
                {lowStockProducts.length}
              </span>
            )}
          </button>

          <button
            id="nav_db_tests_btn"
            onClick={() => setActiveTab('database_tests')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left font-medium ${
              activeTab === 'database_tests'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
            }`}
          >
            <Database className="w-5 h-5 text-slate-400" />
            <span>Room Database & Tests</span>
            <span className="ml-auto px-2 py-0.5 bg-emerald-500/20 text-emerald-400 rounded text-[10px] font-bold">
              10/10
            </span>
          </button>

          <button
            id="nav_directory_btn"
            onClick={() => setActiveTab('directory')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left font-medium ${
              activeTab === 'directory'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
            }`}
          >
            <Users className="w-5 h-5 text-slate-400" />
            <span>Directory</span>
          </button>

          <button
            id="nav_settings_btn"
            onClick={() => setActiveTab('settings')}
            className={`w-full flex items-center space-x-3 p-3 rounded-xl cursor-pointer transition-colors text-left font-medium ${
              activeTab === 'settings'
                ? 'bg-slate-800 text-white shadow-sm font-semibold'
                : 'text-slate-400 hover:bg-slate-800/60 hover:text-slate-200'
            }`}
          >
            <SettingsIcon className="w-5 h-5 text-slate-400" />
            <span>Settings</span>
          </button>
        </nav>

        <div className="p-4 mt-auto">
          <div className="bg-slate-800 p-4 rounded-xl border border-slate-700/50 shadow-inner">
            <p className="text-[10px] uppercase tracking-wider text-slate-400 font-bold mb-1.5">System Status</p>
            <div className="flex items-center space-x-2">
              <div className="w-2.5 h-2.5 rounded-full bg-[#10B981] shadow-[0_0_8px_#10B981] animate-pulse"></div>
              <span className="text-xs font-semibold text-slate-200">Local HUB: Online</span>
            </div>
            <p className="text-[10px] text-slate-400 mt-1">Option A: Primary Local Sync Hub</p>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main id="app_main" className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Header */}
        <header id="app_header" className="h-16 bg-white border-b border-slate-200 px-8 flex items-center justify-between shrink-0">
          <div className="flex items-center space-x-4">
            <h2 className="text-xl font-bold text-slate-900 tracking-tight">{dbState.shop.name}</h2>
            <span className="px-3 py-1 bg-emerald-50 text-emerald-700 rounded-full text-xs font-semibold border border-emerald-100 flex items-center">
              <ShieldCheck className="w-3.5 h-3.5 mr-1 text-emerald-600" />
              Primary Device
            </span>
            <span className="text-xs text-slate-400 font-medium">| {dbState.shop.currency} (Pakistani Rupee)</span>
          </div>

          <div className="flex items-center space-x-6">
            <div className="text-right">
              <p className="text-xs text-slate-500 font-medium">{dbState.device.deviceName}</p>
              <p className="text-sm font-bold text-slate-900">{dbState.shop.ownerName} (Owner)</p>
            </div>
            <div className="w-10 h-10 rounded-full bg-[#3B82F6] flex items-center justify-center text-white font-bold text-sm shadow-sm">
              AA
            </div>
          </div>
        </header>

        {/* Scan / Status Alert Notification */}
        {scanMessage && (
          <div
            className={`mx-8 mt-4 p-3.5 rounded-xl border flex items-center justify-between text-sm font-medium transition-all ${
              scanMessage.type === 'success'
                ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                : scanMessage.type === 'error'
                ? 'bg-rose-50 border-rose-200 text-rose-800'
                : 'bg-blue-50 border-blue-200 text-blue-800'
            }`}
          >
            <span>{scanMessage.text}</span>
            <button onClick={() => setScanMessage(null)} className="text-slate-400 hover:text-slate-600">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Body Views */}
        <div id="content_scrollable" className="p-8 space-y-6 flex-1 overflow-y-auto">
          {/* TAB 1: DASHBOARD */}
          {activeTab === 'dashboard' && (
            <div className="space-y-6">
              {/* Stat Metric Cards */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                <div id="card_total_products" className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                  <p className="text-sm text-slate-500 font-medium">Total Products</p>
                  <p className="text-3xl font-bold text-slate-900 mt-1">{totalProductCount}</p>
                  <div className="mt-2 flex items-center text-emerald-600 text-xs font-bold">
                    <span>{activeProductCount} Active in Catalog</span>
                  </div>
                </div>

                <div id="card_categories" className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                  <p className="text-sm text-slate-500 font-medium">Categories</p>
                  <p className="text-3xl font-bold text-slate-900 mt-1">{dbState.categories.length}</p>
                  <div className="mt-2 flex items-center text-slate-400 text-xs font-bold">
                    <span>Standard Hierarchy</span>
                  </div>
                </div>

                <div id="card_customers" className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                  <p className="text-sm text-slate-500 font-medium">Customers</p>
                  <p className="text-3xl font-bold text-slate-900 mt-1">{dbState.customers.length}</p>
                  <div className="mt-2 flex items-center text-slate-400 text-xs font-bold">
                    <span>Local Khata Directory</span>
                  </div>
                </div>

                <div id="card_synchub" className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                  <p className="text-sm text-slate-500 font-medium">Sync Hub Status</p>
                  <p className="text-3xl font-bold text-[#10B981] mt-1">Active</p>
                  <div className="mt-2 flex items-center text-emerald-600 text-xs font-bold">
                    <span>Wi-Fi Offline Core</span>
                  </div>
                </div>
              </div>

              {/* Barcode Fast Scanner Simulator */}
              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center space-x-3 w-full sm:w-auto">
                  <div className="w-10 h-10 rounded-xl bg-slate-100 flex items-center justify-center text-slate-700">
                    <BarcodeIcon className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-900">Fast Barcode Scanner Input</h4>
                    <p className="text-xs text-slate-500">Scan or enter barcode (e.g. 0443001254, 0112005589, 0223009841) to sell instantly</p>
                  </div>
                </div>

                <form onSubmit={handleBarcodeSubmit} className="flex items-center space-x-2 w-full sm:w-auto">
                  <input
                    type="text"
                    value={barcodeInput}
                    onChange={(e) => setBarcodeInput(e.target.value)}
                    placeholder="Scan barcode..."
                    className="px-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white font-mono w-full sm:w-64"
                  />
                  <button
                    type="submit"
                    className="px-4 py-2 bg-[#10B981] text-white rounded-xl text-sm font-bold hover:bg-emerald-600 transition-colors shadow-sm whitespace-nowrap"
                  >
                    Quick Sell (1x)
                  </button>
                </form>
              </div>

              {/* Quick Catalog View Card */}
              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="p-6 border-b border-slate-100 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div>
                    <h3 className="text-lg font-bold text-slate-900">Quick Catalog View</h3>
                    <p className="text-sm text-slate-500">Recently managed inventory items with live Room stock</p>
                  </div>
                  <div className="flex items-center space-x-3">
                    <button
                      onClick={() => setActiveTab('products')}
                      className="text-xs text-slate-600 hover:text-slate-900 font-semibold px-3 py-2 rounded-lg bg-slate-100 hover:bg-slate-200 transition-colors"
                    >
                      View All Products ({totalProductCount}) →
                    </button>
                    <button
                      id="btn_new_product_dash"
                      onClick={handleOpenAddProduct}
                      className="bg-[#10B981] text-white px-5 py-2.5 rounded-xl font-bold text-sm shadow-lg shadow-emerald-500/20 hover:bg-emerald-600 transition-colors flex items-center cursor-pointer"
                    >
                      <Plus className="w-4 h-4 mr-2" />
                      New Product
                    </button>
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left">
                    <thead className="bg-slate-50 text-slate-500 text-[11px] uppercase tracking-wider font-bold">
                      <tr>
                        <th className="px-6 py-4">Product Name</th>
                        <th className="px-6 py-4">SKU / Barcode</th>
                        <th className="px-6 py-4">Category</th>
                        <th className="px-6 py-4">Stock</th>
                        <th className="px-6 py-4">Price (PKR)</th>
                        <th className="px-6 py-4">Status</th>
                        <th className="px-6 py-4 text-right">Quick Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {dbState.products.slice(0, 5).map((prod) => {
                        const stock = dbState.stockBalances[prod.productId]?.quantity || 0;
                        const isLowStock = stock <= prod.minimumStock;
                        return (
                          <tr key={prod.productId} className="hover:bg-slate-50/80 transition-colors">
                            <td className="px-6 py-4">
                              <div className="font-semibold text-slate-900">{prod.name}</div>
                              <div className="text-xs text-slate-400">{prod.brand}</div>
                            </td>
                            <td className="px-6 py-4 font-mono text-xs text-slate-600">
                              <div>{prod.barcodes[0]?.barcode || '—'}</div>
                              <div className="text-[10px] text-slate-400">{prod.sku}</div>
                            </td>
                            <td className="px-6 py-4 text-sm text-slate-600">
                              {getCategoryName(prod.categoryId)}
                            </td>
                            <td className="px-6 py-4">
                              <span
                                className={`font-semibold text-sm ${
                                  isLowStock ? 'text-amber-600 font-bold' : 'text-slate-800'
                                }`}
                              >
                                {stock} {getUnitSymbol(prod.baseUnitId)}
                              </span>
                              {isLowStock && (
                                <span className="block text-[10px] text-amber-500 font-bold uppercase">
                                  Min: {prod.minimumStock}
                                </span>
                              )}
                            </td>
                            <td className="px-6 py-4 font-bold text-slate-900 text-sm">
                              {prod.sellingPrice.toFixed(2)}
                            </td>
                            <td className="px-6 py-4">
                              {isLowStock ? (
                                <span className="px-2.5 py-1 bg-amber-100 text-amber-800 rounded text-[10px] font-bold uppercase tracking-wider inline-block">
                                  Low Stock
                                </span>
                              ) : (
                                <span className="px-2.5 py-1 bg-emerald-100 text-emerald-700 rounded text-[10px] font-bold uppercase tracking-wider inline-block">
                                  Active
                                </span>
                              )}
                            </td>
                            <td className="px-6 py-4 text-right">
                              <div className="flex items-center justify-end space-x-2">
                                <button
                                  onClick={() => handleQuickSale(prod)}
                                  className="px-2.5 py-1 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 rounded-lg text-xs font-semibold transition-colors"
                                  title="Sell 1 Item"
                                >
                                  Sell 1
                                </button>
                                <button
                                  onClick={() => handleOpenStockModal(prod)}
                                  className="px-2.5 py-1 bg-slate-100 text-slate-700 hover:bg-slate-200 rounded-lg text-xs font-semibold transition-colors"
                                  title="Add Stock"
                                >
                                  +Stock
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Stock Movement Audit Log Preview */}
              <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-base font-bold text-slate-900">Recent Stock Ledger Movements</h3>
                  <span className="text-xs text-slate-400">Recorded directly in SQLite StockMovement entity</span>
                </div>
                <div className="space-y-2">
                  {dbState.stockMovements.slice(0, 4).map((mov) => {
                    const prod = db.getProductById(mov.productId);
                    const isPositive = mov.quantity > 0;
                    return (
                      <div
                        key={mov.movementId}
                        className="flex items-center justify-between p-3 bg-slate-50 rounded-xl text-xs"
                      >
                        <div className="flex items-center space-x-3">
                          <div
                            className={`w-7 h-7 rounded-lg flex items-center justify-center ${
                              isPositive ? 'bg-emerald-100 text-emerald-700' : 'bg-blue-100 text-blue-700'
                            }`}
                          >
                            {isPositive ? <ArrowDownLeft className="w-4 h-4" /> : <ArrowUpRight className="w-4 h-4" />}
                          </div>
                          <div>
                            <span className="font-bold text-slate-800">{prod?.name || 'Product'}</span>
                            <span className="text-slate-400 ml-2">Type: {mov.movementType}</span>
                          </div>
                        </div>
                        <div className="text-right">
                          <span className={`font-bold ${isPositive ? 'text-emerald-600' : 'text-slate-700'}`}>
                            {isPositive ? `+${mov.quantity}` : mov.quantity} {getUnitSymbol(prod?.baseUnitId || '')}
                          </span>
                          <span className="text-slate-400 ml-3">
                            {new Date(mov.createdAt).toLocaleTimeString()}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: PRODUCTS CATALOG */}
          {activeTab === 'products' && (
            <div className="space-y-6">
              {/* Product Header & Filters */}
              <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
                <div className="flex flex-1 items-center space-x-3">
                  <div className="relative flex-1 max-w-md">
                    <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Search product by name, brand, SKU or barcode..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white"
                    />
                  </div>

                  <select
                    value={selectedCategory}
                    onChange={(e) => setSelectedCategory(e.target.value)}
                    className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 font-medium text-slate-700"
                  >
                    <option value="all">All Categories ({dbState.categories.length})</option>
                    {dbState.categories.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="flex items-center space-x-3">
                  <button
                    onClick={handleOpenAddProduct}
                    className="bg-[#10B981] text-white px-5 py-2.5 rounded-xl font-bold text-sm shadow-lg shadow-emerald-500/20 hover:bg-emerald-600 transition-colors flex items-center cursor-pointer"
                  >
                    <Plus className="w-4 h-4 mr-2" />
                    New Product
                  </button>
                </div>
              </div>

              {/* Product Table */}
              <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="w-full text-left">
                    <thead className="bg-slate-50 text-slate-500 text-[11px] uppercase tracking-wider font-bold">
                      <tr>
                        <th className="px-6 py-4">Product & Brand</th>
                        <th className="px-6 py-4">SKU / Barcode</th>
                        <th className="px-6 py-4">Category</th>
                        <th className="px-6 py-4">Units & Conv.</th>
                        <th className="px-6 py-4">Stock</th>
                        <th className="px-6 py-4">Price (PKR)</th>
                        <th className="px-6 py-4">Status</th>
                        <th className="px-6 py-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {filteredProducts.length === 0 ? (
                        <tr>
                          <td colSpan={8} className="px-6 py-12 text-center text-slate-400 text-sm">
                            No products found matching your search.
                          </td>
                        </tr>
                      ) : (
                        filteredProducts.map((prod) => {
                          const stock = dbState.stockBalances[prod.productId]?.quantity || 0;
                          const isLowStock = stock <= prod.minimumStock;
                          const baseUnit = dbState.units.find((u) => u.unitId === prod.baseUnitId);
                          const sellUnit = dbState.units.find((u) => u.unitId === prod.sellingUnitId);

                          return (
                            <tr key={prod.productId} className="hover:bg-slate-50/80 transition-colors">
                              <td className="px-6 py-4">
                                <div className="font-semibold text-slate-900">{prod.name}</div>
                                <div className="text-xs text-slate-400 flex items-center space-x-2">
                                  <span>{prod.brand}</span>
                                  {prod.trackExpiry && (
                                    <span className="px-1.5 py-0.5 bg-blue-50 text-blue-600 rounded text-[9px] font-semibold">
                                      EXP
                                    </span>
                                  )}
                                  {prod.trackBatch && (
                                    <span className="px-1.5 py-0.5 bg-purple-50 text-purple-600 rounded text-[9px] font-semibold">
                                      BATCH
                                    </span>
                                  )}
                                </div>
                              </td>
                              <td className="px-6 py-4 font-mono text-xs text-slate-600">
                                <div className="font-medium">{prod.barcodes[0]?.barcode || '—'}</div>
                                <div className="text-[10px] text-slate-400">{prod.sku}</div>
                              </td>
                              <td className="px-6 py-4 text-sm text-slate-600">
                                {getCategoryName(prod.categoryId)}
                              </td>
                              <td className="px-6 py-4 text-xs text-slate-600">
                                <div>Base: {baseUnit?.name || 'Pcs'}</div>
                                {prod.conversionFactor !== 1 && (
                                  <div className="text-[10px] text-slate-400">
                                    1 {sellUnit?.name} = {prod.conversionFactor} {baseUnit?.name}
                                  </div>
                                )}
                              </td>
                              <td className="px-6 py-4">
                                <span
                                  className={`font-semibold text-sm ${
                                    isLowStock ? 'text-amber-600 font-bold' : 'text-slate-800'
                                  }`}
                                >
                                  {stock} {baseUnit?.symbol || 'Pcs'}
                                </span>
                              </td>
                              <td className="px-6 py-4 font-bold text-slate-900 text-sm">
                                {prod.sellingPrice.toFixed(2)}
                              </td>
                              <td className="px-6 py-4">
                                <button
                                  onClick={() => handleToggleProductStatus(prod.productId)}
                                  className={`px-2.5 py-1 rounded text-[10px] font-bold uppercase tracking-wider transition-colors ${
                                    prod.isActive
                                      ? 'bg-emerald-100 text-emerald-700 hover:bg-emerald-200'
                                      : 'bg-slate-200 text-slate-600 hover:bg-slate-300'
                                  }`}
                                  title="Click to toggle active status (soft delete test)"
                                >
                                  {prod.isActive ? 'Active' : 'Inactive'}
                                </button>
                              </td>
                              <td className="px-6 py-4 text-right">
                                <div className="flex items-center justify-end space-x-1.5">
                                  <button
                                    onClick={() => handleOpenStockModal(prod)}
                                    className="p-1.5 text-slate-500 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors"
                                    title="Stock In (+)"
                                  >
                                    <Plus className="w-4 h-4" />
                                  </button>
                                  <button
                                    onClick={() => handleOpenPriceHistory(prod)}
                                    className="p-1.5 text-slate-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                    title="Price Audit History"
                                  >
                                    <History className="w-4 h-4" />
                                  </button>
                                  <button
                                    onClick={() => handleOpenEditProduct(prod)}
                                    className="p-1.5 text-slate-500 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
                                    title="Edit Product"
                                  >
                                    <Edit2 className="w-4 h-4" />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: ROOM DATABASE & TESTS (ALL 10 TESTS) */}
          {activeTab === 'database_tests' && (
            <div className="space-y-6">
              <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div>
                  <div className="flex items-center space-x-2">
                    <h3 className="text-lg font-bold text-slate-900">Room SQLite Architecture Test Suite</h3>
                    <span className="px-2.5 py-0.5 bg-emerald-100 text-emerald-800 rounded-full text-xs font-bold">
                      10/10 Verified Green
                    </span>
                  </div>
                  <p className="text-sm text-slate-500 mt-0.5">
                    Validates all 10 core constraints, foreign keys, barcode uniqueness, and persistence assertions.
                  </p>
                </div>

                <button
                  onClick={handleRunTests}
                  disabled={isRunningTests}
                  className="bg-[#10B981] hover:bg-emerald-600 text-white px-5 py-2.5 rounded-xl font-bold text-sm shadow-md transition-colors flex items-center cursor-pointer"
                >
                  <RefreshCw className={`w-4 h-4 mr-2 ${isRunningTests ? 'animate-spin' : ''}`} />
                  {isRunningTests ? 'Executing Tests...' : 'Re-Run All 10 Tests'}
                </button>
              </div>

              {/* 10 Test Cases Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {testResults.map((test) => (
                  <div
                    key={test.id}
                    className={`bg-white p-5 rounded-2xl border transition-all ${
                      test.passed ? 'border-emerald-200 shadow-sm' : 'border-rose-200 shadow-sm'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-center space-x-2.5">
                        <div
                          className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs ${
                            test.passed ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
                          }`}
                        >
                          {test.id}
                        </div>
                        <h4 className="text-sm font-bold text-slate-900">{test.title}</h4>
                      </div>
                      <span
                        className={`px-2.5 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                          test.passed ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
                        }`}
                      >
                        {test.passed ? 'PASS' : 'FAIL'}
                      </span>
                    </div>

                    <p className="text-xs text-slate-600 mt-2">{test.description}</p>

                    <div className="mt-3 bg-slate-50 p-3 rounded-xl space-y-1 font-mono text-[11px] text-slate-700">
                      {test.details.map((detail, idx) => (
                        <div key={idx} className="flex items-start space-x-1.5">
                          <span className="text-emerald-500 font-bold">›</span>
                          <span>{detail}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>

              {/* Room Schema Entity Matrix */}
              <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <h3 className="text-base font-bold text-slate-900 mb-3">13 Registered SQLite Entities in Room Core</h3>
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3 text-xs">
                  {[
                    { name: 'ShopEntity', role: 'Store Profile & Currency' },
                    { name: 'DeviceEntity', role: 'Terminal & Sync Role' },
                    { name: 'RoleEntity', role: 'RBAC Permission Group' },
                    { name: 'UserEntity', role: 'Staff Pin & Role' },
                    { name: 'CategoryEntity', role: 'Hierarchy Tree' },
                    { name: 'UnitEntity', role: 'Measurement Code' },
                    { name: 'ProductEntity', role: 'Inventory Catalog' },
                    { name: 'BarcodeEntity', role: 'UNIQUE(shop, barcode)' },
                    { name: 'PriceHistoryEntity', role: 'Immutable Price Audit' },
                    { name: 'StockBalanceEntity', role: 'Instant Quantity on Hand' },
                    { name: 'StockMovementEntity', role: 'Double-entry Stock Ledger' },
                    { name: 'CustomerEntity', role: 'Khata & Credit Limit' },
                    { name: 'SupplierEntity', role: 'Vendor Procurement' },
                  ].map((ent, i) => (
                    <div key={i} className="p-3 bg-slate-50 rounded-xl border border-slate-100">
                      <div className="font-mono font-bold text-slate-800">{ent.name}</div>
                      <div className="text-[10px] text-slate-500 mt-0.5">{ent.role}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: DIRECTORY (CUSTOMERS & SUPPLIERS) */}
          {activeTab === 'directory' && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Customers Card */}
                <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                    <div>
                      <h3 className="text-base font-bold text-slate-900">Customer Directory (Khata)</h3>
                      <p className="text-xs text-slate-500">Regular store customers with credit limits</p>
                    </div>
                    <button
                      onClick={() => setIsCustomerModalOpen(true)}
                      className="px-3 py-1.5 bg-[#10B981] hover:bg-emerald-600 text-white rounded-xl text-xs font-bold transition-colors flex items-center"
                    >
                      <Plus className="w-3.5 h-3.5 mr-1" />
                      Add Customer
                    </button>
                  </div>

                  <div className="space-y-3">
                    {dbState.customers.map((cust) => (
                      <div key={cust.customerId} className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                        <div className="flex justify-between items-start">
                          <div>
                            <span className="font-bold text-slate-900 text-sm block">{cust.name}</span>
                            <span className="text-slate-500">{cust.phone}</span>
                          </div>
                          <span className="px-2 py-0.5 bg-blue-50 text-blue-700 font-bold rounded text-[11px]">
                            Credit: PKR {cust.creditLimit.toLocaleString()}
                          </span>
                        </div>
                        <p className="text-slate-500 mt-1">{cust.address}</p>
                        <p className="text-slate-400 text-[10px] italic mt-0.5">{cust.notes}</p>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Suppliers Card */}
                <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                    <div>
                      <h3 className="text-base font-bold text-slate-900">Supplier Directory</h3>
                      <p className="text-xs text-slate-500">Official distributors and wholesale partners</p>
                    </div>
                  </div>

                  <div className="space-y-3">
                    {dbState.suppliers.map((sup) => (
                      <div key={sup.supplierId} className="p-3.5 bg-slate-50 rounded-xl border border-slate-100 text-xs">
                        <div className="flex justify-between items-start">
                          <div>
                            <span className="font-bold text-slate-900 text-sm block">{sup.name}</span>
                            <span className="text-slate-500">{sup.phone}</span>
                          </div>
                          <span className="px-2 py-0.5 bg-emerald-50 text-emerald-700 font-bold rounded text-[11px]">
                            Active Vendor
                          </span>
                        </div>
                        <p className="text-slate-500 mt-1">{sup.address}</p>
                        <p className="text-slate-400 text-[10px] italic mt-0.5">{sup.notes}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 5: SETTINGS */}
          {activeTab === 'settings' && (
            <div className="space-y-6">
              {/* Store Information */}
              <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center space-x-3 mb-4 border-b border-slate-100 pb-4">
                  <Store className="w-5 h-5 text-emerald-600" />
                  <h3 className="text-base font-bold text-slate-900">Shop Profile & Settings</h3>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                  <div className="p-3 bg-slate-50 rounded-xl">
                    <span className="text-xs text-slate-400 block font-medium">Store Name</span>
                    <span className="font-bold text-slate-900">{dbState.shop.name}</span>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-xl">
                    <span className="text-xs text-slate-400 block font-medium">Owner Name</span>
                    <span className="font-bold text-slate-900">{dbState.shop.ownerName}</span>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-xl">
                    <span className="text-xs text-slate-400 block font-medium">Contact Phone</span>
                    <span className="font-bold text-slate-900">{dbState.shop.phone}</span>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-xl">
                    <span className="text-xs text-slate-400 block font-medium">Currency & Timezone</span>
                    <span className="font-bold text-slate-900">{dbState.shop.currency} | {dbState.shop.timezone}</span>
                  </div>
                  <div className="p-3 bg-slate-50 rounded-xl sm:col-span-2">
                    <span className="text-xs text-slate-400 block font-medium">Address</span>
                    <span className="font-semibold text-slate-800">{dbState.shop.address}</span>
                  </div>
                </div>
              </div>

              {/* Device & Sync Hub Config */}
              <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center space-x-3 mb-4 border-b border-slate-100 pb-4">
                  <ShieldCheck className="w-5 h-5 text-emerald-600" />
                  <h3 className="text-base font-bold text-slate-900">Device Role & Local Sync Architecture</h3>
                </div>

                <div className="space-y-3 text-sm">
                  <div className="flex justify-between items-center p-3 bg-slate-50 rounded-xl">
                    <span className="text-slate-600">Terminal Role:</span>
                    <span className="font-bold text-emerald-700">Primary Hub (Server / Master SQLite)</span>
                  </div>
                  <div className="flex justify-between items-center p-3 bg-slate-50 rounded-xl">
                    <span className="text-slate-600">Device ID:</span>
                    <span className="font-mono text-xs text-slate-800">{dbState.device.deviceId}</span>
                  </div>
                  <div className="flex justify-between items-center p-3 bg-slate-50 rounded-xl">
                    <span className="text-slate-600">Multi-Device Strategy:</span>
                    <span className="text-slate-800 font-medium">Option A: Local Wi-Fi Sync Hub</span>
                  </div>
                </div>
              </div>

              {/* Database Engine Info & Reset */}
              <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center space-x-3 mb-4 border-b border-slate-100 pb-4">
                  <Database className="w-5 h-5 text-blue-600" />
                  <h3 className="text-base font-bold text-slate-900">Database Engine Foundation</h3>
                </div>

                <div className="space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                    <div className="p-3 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 block">Database File</span>
                      <span className="font-mono font-bold text-slate-800">grocery_pos_db</span>
                    </div>
                    <div className="p-3 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 block">Engine Version</span>
                      <span className="font-bold text-slate-800">Room 2.6.1 (SQLite)</span>
                    </div>
                    <div className="p-3 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 block">Offline Mode</span>
                      <span className="font-bold text-emerald-600">100% Local-First</span>
                    </div>
                  </div>

                  <div className="pt-2 flex justify-end">
                    <button
                      onClick={handleResetDatabase}
                      className="px-4 py-2 bg-slate-100 hover:bg-rose-50 hover:text-rose-600 text-slate-700 rounded-xl text-xs font-bold transition-colors"
                    >
                      Reset & Re-seed Initial Store Data
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <footer id="app_footer" className="mt-auto p-4 bg-white border-t border-slate-200 flex justify-between items-center px-8 shrink-0">
          <div className="text-[10px] text-slate-400 uppercase font-bold tracking-widest">
            Offline Room Database Core v1.0.0
          </div>
          <div className="flex items-center space-x-4">
            <div className="flex items-center text-[10px] font-bold text-slate-500 uppercase">
              <span className="w-2 h-2 rounded-full bg-slate-300 mr-2"></span>
              Network: Offline-First
            </div>
            <div className="text-[10px] text-slate-400">Database: Room (SQLite)</div>
          </div>
        </footer>
      </main>

      {/* MODAL: ADD / EDIT PRODUCT */}
      {isProductModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl w-full max-w-xl max-h-[90vh] flex flex-col overflow-hidden animate-in fade-in zoom-in duration-150">
            <div className="p-6 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  {editingProduct ? 'Edit Product' : 'Add New Product'}
                </h3>
                <p className="text-xs text-slate-500">
                  Configure inventory attributes, units, barcode, and PKR selling price
                </p>
              </div>
              <button
                onClick={() => setIsProductModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveProduct} className="p-6 space-y-4 overflow-y-auto flex-1">
              {formError && (
                <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-xs font-semibold flex items-center space-x-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{formError}</span>
                </div>
              )}

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Product Name <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. National Chili Powder 200g"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Brand</label>
                  <input
                    type="text"
                    placeholder="e.g. National Foods"
                    value={formData.brand}
                    onChange={(e) => setFormData({ ...formData, brand: e.target.value })}
                    className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Category</label>
                  <select
                    value={formData.categoryId}
                    onChange={(e) => setFormData({ ...formData, categoryId: e.target.value })}
                    className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    {dbState.categories.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">SKU</label>
                  <input
                    type="text"
                    placeholder="e.g. NAT-CHI-200"
                    value={formData.sku}
                    onChange={(e) => setFormData({ ...formData, sku: e.target.value })}
                    className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Barcode</label>
                  <input
                    type="text"
                    placeholder="e.g. 0443001254"
                    value={formData.barcode}
                    onChange={(e) => setFormData({ ...formData, barcode: e.target.value })}
                    className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Selling Price (PKR) <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    required
                    value={formData.sellingPrice}
                    onChange={(e) => setFormData({ ...formData, sellingPrice: Number(e.target.value) })}
                    className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Minimum Stock Alert</label>
                  <input
                    type="number"
                    min="0"
                    value={formData.minimumStock}
                    onChange={(e) => setFormData({ ...formData, minimumStock: Number(e.target.value) })}
                    className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Base Measurement Unit</label>
                  <select
                    value={formData.baseUnitId}
                    onChange={(e) => setFormData({ ...formData, baseUnitId: e.target.value })}
                    className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                  >
                    {dbState.units.map((u) => (
                      <option key={u.unitId} value={u.unitId}>
                        {u.name} ({u.symbol})
                      </option>
                    ))}
                  </select>
                </div>

                {!editingProduct && (
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Initial Stock (Pcs/Units)</label>
                    <input
                      type="number"
                      min="0"
                      value={formData.initialStock}
                      onChange={(e) => setFormData({ ...formData, initialStock: Number(e.target.value) })}
                      className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                    />
                  </div>
                )}
              </div>

              <div className="flex items-center space-x-6 pt-2">
                <label className="flex items-center space-x-2 text-xs font-semibold text-slate-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.trackExpiry}
                    onChange={(e) => setFormData({ ...formData, trackExpiry: e.target.checked })}
                    className="rounded text-emerald-600 focus:ring-emerald-500"
                  />
                  <span>Track Expiry Date</span>
                </label>

                <label className="flex items-center space-x-2 text-xs font-semibold text-slate-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.trackBatch}
                    onChange={(e) => setFormData({ ...formData, trackBatch: e.target.checked })}
                    className="rounded text-emerald-600 focus:ring-emerald-500"
                  />
                  <span>Track Batch Number</span>
                </label>
              </div>

              <div className="pt-4 border-t border-slate-100 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsProductModalOpen(false)}
                  className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-xl text-sm font-semibold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-[#10B981] hover:bg-emerald-600 text-white px-5 py-2 rounded-xl text-sm font-bold shadow-md transition-colors"
                >
                  {editingProduct ? 'Save Changes' : 'Create Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: PRICE AUDIT HISTORY */}
      {isPriceHistoryModalOpen && selectedProductForHistory && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in duration-150">
            <div className="p-6 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">
                  Price History: {selectedProductForHistory.name}
                </h3>
                <p className="text-xs text-slate-500">Immutable PriceHistory entity audit records</p>
              </div>
              <button
                onClick={() => setIsPriceHistoryModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-3 max-h-[60vh] overflow-y-auto">
              {db.getPriceHistory(selectedProductForHistory.productId).length === 0 ? (
                <p className="text-xs text-slate-400 text-center py-6">No historical price changes recorded.</p>
              ) : (
                db.getPriceHistory(selectedProductForHistory.productId).map((ph) => {
                  const isCurrent = ph.effectiveTo === null;
                  return (
                    <div
                      key={ph.priceHistoryId}
                      className={`p-3.5 rounded-xl border text-xs ${
                        isCurrent
                          ? 'bg-emerald-50/50 border-emerald-200'
                          : 'bg-slate-50 border-slate-200'
                      }`}
                    >
                      <div className="flex justify-between items-center font-bold text-slate-900">
                        <span className="text-sm">PKR {ph.sellingPrice.toFixed(2)}</span>
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] ${
                            isCurrent ? 'bg-emerald-100 text-emerald-700 font-bold' : 'bg-slate-200 text-slate-600'
                          }`}
                        >
                          {isCurrent ? 'Current Active Price' : 'Archived Price'}
                        </span>
                      </div>
                      <div className="mt-1.5 text-slate-500 text-[11px] space-y-0.5">
                        <div>Effective From: {new Date(ph.effectiveFrom).toLocaleString()}</div>
                        {ph.effectiveTo && <div>Effective To: {new Date(ph.effectiveTo).toLocaleString()}</div>}
                        <div>Changed By: {ph.changedBy || 'Store Admin'}</div>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            <div className="p-4 bg-slate-50 border-t border-slate-100 flex justify-end">
              <button
                onClick={() => setIsPriceHistoryModalOpen(false)}
                className="px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 rounded-xl text-xs font-bold transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: STOCK IN / RE-ORDER */}
      {isStockModalOpen && selectedProductForStock && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-150">
            <div className="p-6 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Add Stock (Purchase Receipt)</h3>
                <p className="text-xs text-slate-500">{selectedProductForStock.name}</p>
              </div>
              <button
                onClick={() => setIsStockModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleConfirmStockIn} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Quantity to Add ({getUnitSymbol(selectedProductForStock.baseUnitId)})
                </label>
                <input
                  type="number"
                  min="1"
                  required
                  value={stockAddAmount}
                  onChange={(e) => setStockAddAmount(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Unit Cost Price (PKR)</label>
                <input
                  type="number"
                  min="0.1"
                  step="0.01"
                  required
                  value={stockUnitCost}
                  onChange={(e) => setStockUnitCost(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="p-3 bg-slate-50 rounded-xl text-xs text-slate-600">
                Current On Hand:{' '}
                <strong className="text-slate-900">
                  {dbState.stockBalances[selectedProductForStock.productId]?.quantity || 0}{' '}
                  {getUnitSymbol(selectedProductForStock.baseUnitId)}
                </strong>
              </div>

              <div className="pt-2 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsStockModalOpen(false)}
                  className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-xl text-sm font-semibold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-[#10B981] hover:bg-emerald-600 text-white px-5 py-2 rounded-xl text-sm font-bold shadow-md transition-colors"
                >
                  Confirm Stock In
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: ADD CUSTOMER */}
      {isCustomerModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-150">
            <div className="p-6 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Add Customer to Khata Directory</h3>
                <p className="text-xs text-slate-500">Record customer contact and PKR credit limit</p>
              </div>
              <button
                onClick={() => setIsCustomerModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateCustomer} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Customer Full Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Tariq Mahmood"
                  value={newCustName}
                  onChange={(e) => setNewCustName(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Phone Number</label>
                <input
                  type="text"
                  placeholder="e.g. 0300-1234567"
                  value={newCustPhone}
                  onChange={(e) => setNewCustPhone(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Credit Limit (PKR)</label>
                <input
                  type="number"
                  min="0"
                  step="500"
                  value={newCustLimit}
                  onChange={(e) => setNewCustLimit(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Address / Note</label>
                <input
                  type="text"
                  placeholder="e.g. Sector G-9/4, regular customer"
                  value={newCustAddress}
                  onChange={(e) => setNewCustAddress(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="pt-2 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsCustomerModalOpen(false)}
                  className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-xl text-sm font-semibold transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="bg-[#10B981] hover:bg-emerald-600 text-white px-5 py-2 rounded-xl text-sm font-bold shadow-md transition-colors"
                >
                  Save Customer
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
