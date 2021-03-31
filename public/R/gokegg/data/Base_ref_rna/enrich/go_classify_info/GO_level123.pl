#!usr/bin/perl -w

use strict;

die "usage:perl $0 <GO _relat> \n" unless @ARGV == 2;

open A,$ARGV[0];
my %h;

my %rela1;  my %rela2;  my %rela3;
my %rela;
while(<A>){
    chomp;my @a=split /\t+/;
    $h{$a[0]}=$a[-1];
    $rela{$a[1]}{$a[0]}=1;
    if($a[1] eq "GO:0008150"){
        $rela1{$a[0]}=1;
    }
    if($a[1] eq "GO:0003674"){
        $rela2{$a[0]}=1;
    }
    if($a[1] eq "GO:0005575"){
        $rela3{$a[0]}=1;
    }
}
close A;

print "GO:0008150\tBP\n";
foreach my $k1(sort keys %rela1){
    print "_\t$k1\t$h{$k1}\n";
    foreach my $k2(sort keys %{$rela{$k1}}){
        print "_\t_\t$k2\t$h{$k2}\n";
    }
}

print "GO:0003674\tMF\n";
foreach my $k1(sort keys %rela2){
    print "_\t$k1\t$h{$k1}\n";
    foreach my $k2(sort keys %{$rela{$k1}}){
        print "_\t_\t$k2\t$h{$k2}\n";
    }
}

print "GO:0005575\tCC\n";
foreach my $k1(sort keys %rela3){
    print "_\t$k1\t$h{$k1}\n";
    foreach my $k2(sort keys %{$rela{$k1}}){
        print "_\t_\t$k2\t$h{$k2}\n";
    }
}

