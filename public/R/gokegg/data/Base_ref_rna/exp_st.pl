#!usr/bin/perl -w

use strict;

die "usage:perl $0 <genes.rpkm.txt>  <output:file>\n" unless @ARGV == 2;


open B,$ARGV[0];
open OUT4,">$ARGV[1]";
my %list;my %exp;
my $head=<B>;	chomp $head;my @sam=split /\s+/,$head;	my @site=qw/0 1 5 10/;
while(<B>){
	chomp;my @a=split; $list{$a[0]}=1;
	foreach my $k(1..$#a){
		if($a[$k] > 0){			$exp{$sam[$k]}{0}++;		}
		if($a[$k] > 1){			$exp{$sam[$k]}{1}++;		}
		if($a[$k] > 5){			$exp{$sam[$k]}{5}++;		}
		if($a[$k] > 10){		$exp{$sam[$k]}{10}++;		}
	}
}
close B;
print OUT4 "Sam";
foreach my $k1(@site){
	print OUT4 "\tfpkm>$k1";
}
print OUT4 "\n";
foreach my $k1(1..$#sam){
	print OUT4 "$sam[$k1]";
	foreach my $k2(@site){
		print OUT4 "\t$exp{$sam[$k1]}{$k2}";
	}
	print OUT4 "\n";
}
